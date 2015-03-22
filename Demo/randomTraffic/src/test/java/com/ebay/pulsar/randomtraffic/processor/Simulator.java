/*
Pulsar
Copyright (C) 2013-2015 eBay Software Foundation
Licensed under the GPL v2 license.  See LICENSE for full terms.
*/
package com.ebay.pulsar.randomtraffic.processor;

/*
public class Simulator {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1; i++) {
            sendMessage();
        }
    }

    private static void sendMessage() throws IOException, JsonProcessingException, JsonGenerationException,
            JsonMappingException, UnsupportedEncodingException, HttpException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> m = new HashMap<String, Object>();

        m.put("si", "12345");
        m.put("ct", System.currentTimeMillis());

        String payload = mapper.writeValueAsString(m);
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8080/tracking/ingest/PulsarRawEvent");
//      method.addRequestHeader("Accept-Encoding", "gzip,deflate,sdch");
        method.setRequestEntity(new StringRequestEntity(payload, "application/json", "UTF-8"));
        int status = client.executeMethod(method);
        System.out.println(Arrays.toString(method.getResponseHeaders()));
        System.out.println("Status code: " + status + ", Body: " +  method.getResponseBodyAsString());
    }
}

*/
