/*
Pulsar
Copyright (C) 2013-2015 eBay Software Foundation
Licensed under the GPL v2 license.  See LICENSE for full terms.
*/

package com.ebay.pulsar.randomtraffic.processor;

import com.ebay.jetstream.event.EventException;
import com.ebay.jetstream.event.JetstreamEvent;
import com.ebay.jetstream.event.support.AbstractEventProcessor;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;


@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ManagedResource(objectName = "Event/Processor/Simulator", description = "Event Simulator")
public class RandomTrafficGenerator extends AbstractEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomTrafficGenerator.class.getPackage().getName());

    /**
     * Random data source
     */
    private String simulatorFilePath;
    private String ipFilePath;
    private String uaFilePath;
    private String itemsFilePath;
    private String refererFilePath;

    /**
     * Connection information
     */
    private String collectorEventType;
    private String collectorUri;
    private String collectorBatchUri;
    private String collectorNodeHost;
    private String collectorNodePort;

    /**
     *
     */
    private boolean m_runFlag;

    /**
     * Batch mode flag
     */
    private boolean batchMode;
    private long m_batchSize;

    /**
     *
     */
    private int siCount;

    private int minVolume;

    private double peakTimes;


    private HttpClient m_client;

    private int m_client_retry_count = 10;

    private PostMethod m_method;

    private String m_payload = "";

    private List m_guidList = new ArrayList<RawSource>();



    static List<String> m_ipList = new ArrayList<String>();

    static List<String> m_uaList = new ArrayList<String>();

    static List<String> m_itemList = new ArrayList<String>();

    static List<String> m_refererList = new ArrayList<String>();

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void sendEvent(JetstreamEvent event) throws EventException {}

    @Override
    public int getPendingEvents() {
        return 0;
    }

    @Override
    public void shutDown() {
        if (m_method != null) {
            m_method.releaseConnection();
        }
    }

    @ManagedOperation
    @Scheduled(fixedRate=2000)
    public void generate() {

        // Max sleep default
        int sleepMax = 1000;

        while (true) {
            try {
                if (m_runFlag) {
                    String payload;
                    if (batchMode) {
                        adjustBatchSize();
                        payload = buildPayload(m_payload, m_batchSize);
                        m_method.setRequestBody(payload);
                        sleepMax = 5000;
                    } else {
                        m_batchSize = 1;
                        payload = buildPayload(m_payload, 1);
                        m_method.setRequestBody(payload);
                        sleepMax = 1000;
                    }
                    LOGGER.trace(payload);
                    m_client.executeMethod(m_method);
                }
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                m_method.releaseConnection();
                m_method.removeRequestHeader("Content-Length");
            }

            Random random = new Random();
            long sleep = random.nextInt(sleepMax);
            LOGGER.info("sleep = " + sleep);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @ManagedOperation
    public void start() {
        m_runFlag = true;
    }

    @ManagedOperation
    @Override
    public void pause() {
        m_runFlag = false;
    }

    @Override
    protected void processApplicationEvent(ApplicationEvent event) {
        LOGGER.info("Received in processApplicationEvent: " + event.getClass().getName());
    }

    @Override
    public void resume() {
        m_runFlag = true;
    }

    private void init() {

        initList(m_ipList, ipFilePath);
        initList(m_uaList, uaFilePath);
        initList(m_itemList, itemsFilePath);
        initList(m_refererList, refererFilePath);

        initGUIDList();

        String finalURL = "";
        if (batchMode) {
            finalURL = "http://" + collectorNodeHost + ":" + collectorNodePort + collectorBatchUri;
        } else {
            finalURL = "http://" + collectorNodeHost + ":" + collectorNodePort + collectorUri;;
        }

        m_payload = readFromResource();

        HttpClientParams clientParams = new HttpClientParams();
        //http.connection.timeout
        clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5 * 1000);
        //http.socket.timeout
        clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 60 * 1000);
        //@todo http.connection-manager.timeout

        m_client = new HttpClient(clientParams);
        m_client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(m_client_retry_count, true));

        m_method = new PostMethod(finalURL + collectorEventType);
        m_method.setRequestHeader("Connection", "Keep-Alive");
        m_method.setRequestHeader("Accept-Charset", "UTF-8");
    }

    private long trafficVolume(int baseVolume, double peakTimes, int sec) {
        if (baseVolume < 0) {
            throw new IllegalArgumentException("baseVolume should be >= 0!");
        }
        if (peakTimes <= 0) {
            throw new IllegalArgumentException("peakTimes should be > 0!");
        }
        return (long) (baseVolume + (baseVolume * (peakTimes - 1)) * Math.sin(sec * Math.PI / 60));
    }

    private void adjustBatchSize() {
        Date date = new Date();
        int secondValue = date.getSeconds();
        m_batchSize = trafficVolume(minVolume, peakTimes, secondValue);
    }

    private String readFromResource() {
        FileReader in;
        String payload = "";
        try {
            File file = new File(simulatorFilePath);
            in = new FileReader(file);
            BufferedReader r = new BufferedReader(in);
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    payload += line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        return payload;
    }

    private String buildPayload(String payload, long size) {
        Random random = new Random();

        StringBuffer strBuffer = new StringBuffer();

        if (size > 1) {
            strBuffer.append("[");
        }

        for (int i = 0; i < size; i++) {

            int randomInt = random.nextInt(siCount);
            RawSource source = (RawSource) m_guidList.get(randomInt);
            strBuffer.append(replaceSource(payload, source));

            if (i < size - 1) {
                strBuffer.append(",");
            }
        }

        if (size > 1) {
            strBuffer.append("]");
        }
        LOGGER.info("payload size = " + size);
        return strBuffer.toString();
    }

    private String replaceSource(String payload, RawSource source) {

        Random random = new Random();

        String payload1 = payload.replace("${siValue}", source.getIndicatorString());
        String payload2 = payload1.replace("${ipValue}", source.getGeoString());
        String payload3 = payload2.replace("${uaValue}", source.getDeviceString());

        String payload4 = payload3.replace("${ctValue}", String.valueOf(System.currentTimeMillis()));

        String[] items = m_itemList.get(random.nextInt(m_itemList.size() - 1)).split(":");
        String payload5 = payload4.replace("${itemTitle}", items[1]);
        String payload6 = payload5.replace("${itemPrice}", items[2]);
        //String payload7 = payload6.replace("${itemCategory}", items[0]);

        String payload8 = payload6.replace("${campaignName}", "Campaign - " + String.valueOf(random.nextInt(20)));
        String payload9 = payload8.replace("${campaignGMV}", String.valueOf(((double) random.nextInt(100000) * 7) / 100.00));
        String payload10 = payload9.replace("${campaignQuantity}", String.valueOf(random.nextInt(100)));

        String payload11 = payload10.replace("${refererValue}", m_refererList.get(random.nextInt(m_refererList.size() - 1)));

        return payload11;
    }

    private void initGUIDList() {
        Random random = new Random();
        for (int i = 0; i < siCount; i++) {
            String key = java.util.UUID.randomUUID().toString();
            m_guidList.add(
                new RawSource(
                    key, m_ipList.get(random.nextInt(m_ipList.size() - 1)),
                    m_uaList.get(random.nextInt(m_uaList.size() - 1)))
            );
        }
    }

    /**
     * Inits data structure based on file - row by row record
     * @param list
     * @param filename
     */
    private void initList(List<String> list, String filename) {
        FileReader in;
        try {
            File file = new File(filename);
            in = new FileReader(file);
            BufferedReader r = new BufferedReader(in);
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    list.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    private class RawSource {

        String geoString;
        String deviceString;
        String indicatorString;

        public RawSource(String indicator, String geo, String device) {
            indicatorString = indicator;
            geoString = geo;
            deviceString = device;
        }

        public String getGeoString() {
            return geoString;
        }

        public void setGeoString(String geo) {
            geoString = geo;
        }

        public String getDeviceString() {
            return deviceString;
        }

        public void setDeviceString(String device) {
            deviceString = device;
        }

        public String getIndicatorString() {
            return indicatorString;
        }

        public void setIndicatorString(String indicator) {
            indicatorString = indicator;
        }

    }
}
