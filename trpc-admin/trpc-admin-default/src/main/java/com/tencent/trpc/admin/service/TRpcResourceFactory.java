/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.admin.service;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * TRPC Resource Factory
 */
public class TRpcResourceFactory implements ResourceFactory {

    private Object resourceInstance;
    private Class<?> scannableClass;
    private String contextPath;

    public TRpcResourceFactory(Class<?> scannableClass, Object resourceInstance,
            String contextPath) {
        this.resourceInstance = resourceInstance;
        this.scannableClass = scannableClass;
        this.contextPath = contextPath;
    }

    @Override
    public Object createResource(HttpRequest request, HttpResponse response,
            ResteasyProviderFactory factory) {
        return resourceInstance;
    }

    @Override
    public Class<?> getScannableClass() {
        return scannableClass;
    }


    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void registered(ResteasyProviderFactory factory) {
    }

    @Override
    public void requestFinished(HttpRequest request, HttpResponse response, Object resource) {
    }

    @Override
    public void unregistered() {
    }

}
