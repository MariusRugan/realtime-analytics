/*
Pulsar
Copyright (C) 2013-2015 eBay Software Foundation
Licensed under the GPL v2 license.  See LICENSE for full terms.
*/
package com.ebay.pulsar.collector.servlet;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.ebay.jetstream.event.support.ErrorManager;

@ManagedResource(objectName = "Event/Channel", description = "IngestServlet")
public class ServletStats {

    private String lastFailedRequest;

    private ErrorManager errorManager;

    private AtomicLong errorCount = new AtomicLong(0);

    private AtomicLong ingestRequestCount = new AtomicLong(0);

    private AtomicLong ingestBatchRequestCount = new AtomicLong(0);

    private AtomicLong invalidRequestCount = new AtomicLong(0);

    @ManagedOperation(description="Clean Errors")
    public void cleanErrors() {
        errorCount.set(0);
        errorManager.clearErrorList();
        lastFailedRequest = null;
    }

    public void setErrorManager(ErrorManager errorManager) {
        this.errorManager = errorManager;
    }

    public long getIngestRequestCount() {
        return ingestRequestCount.get();
    }

    public long getBatchIngestRequestCount() {
        return ingestBatchRequestCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    @ManagedAttribute
    public ErrorManager getErrorManager() {
        return errorManager;
    }

    public String getLastFailedRequest() {
        return lastFailedRequest;
    }

    public void incIngestRequestCount() {
        ingestRequestCount.incrementAndGet();
    }

    public void incBatchIngestRequestCount() {
        ingestBatchRequestCount.incrementAndGet();
    }

    public void registerError(Throwable ex) {
        errorCount.incrementAndGet();
        errorManager.registerError(ex);
    }

    public void setLastFailedRequest(String lastFailedRequest) {
        this.lastFailedRequest = lastFailedRequest;
    }

    public void incInvalidRequestCount() {
        invalidRequestCount.incrementAndGet();
    }

}
