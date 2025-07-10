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

package com.tencent.trpc.core.common.config;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration information for name lookup.
 */
public class NamingOptions {

    public static final String NAMESPACE = "namespace";
    public static final String DESTINATION_SET = "destination_set";
    public static final String SOURCE_SET = "source_set";
    /**
     * Used to associate the corresponding routing plugin.
     */
    protected String selectorId;
    /**
     * Service name.
     */
    protected String serviceNaming;
    /**
     * Extension parameters.
     */
    protected Map<String, Object> extMap = Maps.newHashMap();

    public static String toDirectNamingUrl(String ipport) {
        return "ip://" + ipport;
    }

    public static String toNamingUrl(String selector, String naming) {
        return selector + "://" + naming;
    }

    /**
     * Parse namingUrl to get {@link NamingOptions}.
     *
     * @param url Parsed from <b>namingUrl</b>:
     *         1) polaris://Trpc.xxx?key=value&key=value
     *         2) l5://mid:cid?key=value
     *         3) zk://path?key=value Direct connection scenario configuration:
     *         Direct connection configuration: ip://ip:port?key=value
     * @param namingMap Extension parameter configuration combined with the key/value in the URL, together they
     *         form NamingOptions {@link #extMap}
     */
    public static NamingOptions parseNamingUrl(String url, Map<String, Object> namingMap) {
        url = Objects.requireNonNull(url, "url").trim();
        NamingOptions namingOptions = new NamingOptions();
        int index = url.indexOf("://");
        PreconditionUtils.checkArgument(index >= 0, "invalid uri:%s", url);
        namingOptions.selectorId = url.substring(0, index).toLowerCase();
        int index3 = url.indexOf('?', index + 3);
        if (index3 > 0) {
            namingOptions.serviceNaming = url.substring(index + 3, index3);
        } else {
            namingOptions.serviceNaming = url.substring(index + 3);
        }
        if (index3 > 0) {
            String query = url.substring(index3 + 1);
            String[] querySplits = query.split("&");
            for (String kv : querySplits) {
                String[] kvSplit = kv.split("=");
                namingOptions.extMap.put(kvSplit[0], kvSplit[1]);
            }
        }
        if (namingMap != null) {
            namingOptions.extMap.putAll(namingMap);
        }
        return namingOptions;
    }

    public String getSelectorId() {
        return selectorId;
    }

    public NamingOptions setSelectorId(String selectorId) {
        this.selectorId = selectorId;
        return this;
    }

    public String getServiceNaming() {
        return serviceNaming;
    }

    public NamingOptions setServiceNaming(String serviceNaming) {
        this.serviceNaming = serviceNaming;
        return this;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public NamingOptions setExtMap(Map<String, Object> extMap) {
        this.extMap = extMap;
        return this;
    }

    @Override
    public String toString() {
        return "NamingOptions [selectorId=" + selectorId + ", serviceNaming=" + serviceNaming
                + ", extMap=" + extMap + "]";
    }

}
