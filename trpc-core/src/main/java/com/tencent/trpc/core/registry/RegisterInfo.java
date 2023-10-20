/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.registry;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This is a registration information description class that needs to be registered with the registration center.
 * The most critical information in the middle includes the IP, port, service name, etc. of the service.
 */
public class RegisterInfo implements Comparable<RegisterInfo>, Serializable, Cloneable {

    private static final long serialVersionUID = -5770826614635186498L;
    /**
     * Regular expression for separator。The regular expression should match one or more words or phrases separated
     * by commas. For example, in the string "apple, banana, orange", the words "apple", "banana", and "orange"
     * will be matched.
     */
    private static Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");
    private final String protocol;
    private final String host;
    private final int port;
    private final String serviceName;
    private final Map<String, Object> parameters;
    /**
     * Service group
     **/
    private final String group;
    /**
     * Service version
     **/
    private final String version;
    private transient volatile String identity;
    private transient volatile Map<String, Number> numbers;

    public RegisterInfo() {
        this.protocol = null;
        this.host = null;
        this.port = 0;
        this.group = null;
        this.version = null;
        this.serviceName = null;
        this.parameters = null;
    }

    public RegisterInfo(String protocol, String host, int port) {
        this(protocol, host, port, null, null, null, (Map<String, Object>) null);
    }

    public RegisterInfo(String protocol, String host, int port, String serviceName) {
        this(protocol, host, port, serviceName, null, null, (Map<String, Object>) null);
    }


    public RegisterInfo(String protocol, String host, int port, String serviceName,
            Map<String, Object> parameters) {
        this(protocol, host, port, serviceName, null, null, parameters);
    }

    public RegisterInfo(String protocol, String host, int port, String serviceName, String group,
            Map<String, Object> parameters) {
        this(protocol, host, port, serviceName, group, null, null);
    }

    public RegisterInfo(String protocol, String host, int port, String group, String version, String serviceName) {
        this(protocol, host, port, serviceName, group, version, null);
    }

    public RegisterInfo(String protocol, String host, int port, String serviceName, String group,
            String version, Map<String, Object> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = Math.max(port, 0);
        this.serviceName = serviceName;
        this.version = version;
        this.group = group;
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        this.parameters = parameters;
    }

    /**
     * Decode from URL format to RegisterInfo
     *
     * @param path URL string
     * @return RegisterInfo object
     */
    public static RegisterInfo decode(String path) {
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("registerInfo" + path + " can not encode to registerInfo", e);
        }
        Map<String, Object> params = new HashMap<>();
        int i = path.indexOf("?");
        if (i >= 0) {
            String[] parts = path.substring(i + 1).split("&");
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        params.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        params.put(part, part);
                    }
                }
            }
            path = path.substring(0, i);
        }
        i = path.indexOf("://");
        if (i <= 0) {
            throw new IllegalStateException("path missing protocol: \"" + path + "\"");
        }
        final String protocol = path.substring(0, i);
        path = path.substring(i + 3);
        i = path.indexOf("/");
        if (i <= 0) {
            throw new IllegalStateException("path missing serviceName: \"" + path + "\"");
        }
        final String serviceName = path.substring(i + 1);
        path = path.substring(0, i);
        i = path.indexOf(":");
        if (i <= 0) {
            throw new IllegalStateException("path missing host and port: \"" + path + "\"");
        }
        final String host = path.substring(0, i);
        int port = Integer.parseInt(path.substring(i + 1));
        return new RegisterInfo(protocol, host, port, serviceName, params);
    }

    /**
     * Encode into URL string
     *
     * @param registerInfo RegisterInfo object
     * @return URL string
     */
    public static String encode(RegisterInfo registerInfo) {
        if (registerInfo == null) {
            return "";
        }
        try {
            Map<String, Object> parameters = registerInfo.getParameters();
            String[] params = null;
            if (parameters != null) {
                params = parameters.keySet().toArray(new String[0]);
            }
            String url = registerInfo.buildString(true, true, true, params);
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    "registerInfo" + registerInfo + " can not encode to url", e);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getIdentity() {
        return identity;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getPort(int defaultPort) {
        return port <= 0 ? defaultPort : port;
    }

    public String getAddress() {
        return port <= 0 ? host : host + ":" + port;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public Object getObject(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key) {
        Object value = parameters.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public String[] getParameter(String key, String[] defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return COMMA_SPLIT_PATTERN.split(value);
    }

    public double getParameter(String key, double defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(key, d);
        return d;
    }

    public float getParameter(String key, float defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    public long getParameter(String key, long defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    public int getParameter(String key, int defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public short getParameter(String key, short defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(key, s);
        return s;
    }

    public byte getParameter(String key, byte defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(key, b);
        return b;
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) {
            numbers = new ConcurrentHashMap<String, Number>();
        }
        return numbers;
    }

    public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        identity = buildString(true, true, false);
        return identity;
    }

    /**
     * Build into URL string
     *
     * @param appendIpPort Whether to concatenate IP and port
     * @param appendService Whether to concatenate service name
     * @param appendParameter Whether to concatenate parameters
     * @param params List of parameters to be concatenated
     * @return URL string
     */
    public String buildString(boolean appendIpPort, boolean appendService, boolean appendParameter,
            String... params) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(protocol)) {
            buf.append(protocol);
            buf.append("://");
        }
        if (appendIpPort && StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }

        if (appendService && StringUtils.isNotEmpty(serviceName)) {
            buf.append("/").append(serviceName);
        }

        if (appendParameter) {
            String paramStr = buildParameters(parameters, params);
            if (StringUtils.isNotEmpty(paramStr)) {
                buf.append("?").append(paramStr);
            }
        }
        return buf.toString();
    }

    private String buildParameters(Map<String, Object> parameters, String[] params) {
        StringBuilder paramStr = new StringBuilder();
        if (!ArrayUtils.isEmpty(params)) {
            List<String> paramList = Arrays.asList(params);
            for (Map.Entry<String, Object> entry : new TreeMap<>(parameters).entrySet()) {
                if (entry.getKey() != null && paramList.contains(entry.getKey())) {
                    paramStr.append("&").append(entry.getKey()).append("=");
                    paramStr.append(
                            entry.getValue() == null ? "" : entry.getValue().toString().trim());
                }
            }
        }
        int startIndex = "&".length();
        if (paramStr.length() > 0) {
            return paramStr.substring(startIndex);
        } else {
            return paramStr.toString();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RegisterInfo other = (RegisterInfo) obj;
        return compareObj(host, other.host) && compareObj(parameters, other.parameters)
                && compareObj(serviceName, other.serviceName) && compareObj(protocol, other.protocol)
                && port == other.port;
    }

    private boolean compareObj(Object obj, Object other) {
        if (obj == null && other == null) {
            return true;
        }
        if (obj == null || other == null) {
            return false;
        }
        // The decoded parameter is a string that needs to be processed specially.
        // It needs to be converted to a string before comparison.
        if (obj instanceof Map && other instanceof Map) {
            Map<String, Object> objMap = (Map<String, Object>) obj;
            Map<String, Object> otherMap = (Map<String, Object>) other;
            String[] objParams = objMap.keySet().toArray(new String[0]);
            String[] otherParams = otherMap.keySet().toArray(new String[0]);
            return objMap.size() == otherMap.size()
                    && buildParameters(objMap, objParams)
                    .equals(buildParameters(otherMap, otherParams));
        }
        return obj.equals(other);
    }

    @Override
    public int compareTo(RegisterInfo registerInfo) {
        int i = host.compareTo(registerInfo.host);
        if (i == 0) {
            i = (Integer.compare(port, registerInfo.port));
        }
        return i;
    }

    @Override
    public String toString() {
        return "ServiceInstance{" + "protocol='" + protocol + '\'' + ", host='" + host + '\''
                + ", port=" + port + ", serviceName='" + serviceName + '\'' + ", parameters="
                + parameters
                + ", identity='" + identity + '\'' + ", numbers=" + numbers + '}';
    }

    public RegisterInfo clone() {
        RegisterInfo registerInfo = new RegisterInfo(protocol, host, port, serviceName, group,
                version, new HashMap<>(parameters));
        return registerInfo;
    }

}
