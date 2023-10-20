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

package com.tencent.trpc.proto.http.common;

import com.google.protobuf.Message;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.serialization.support.JSONSerialization;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.proto.http.util.StreamUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpHeaders;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.springframework.cglib.beans.BeanMap;

/**
 * HTTP encoding and decoding utility class, also used in the Spring MVC module.
 */
public class HttpCodec {

    /**
     * Convert http param to PB param
     *
     * @param request HttpServletRequest
     * @param msgType the type of decoded param
     * @return decoded param
     * @throws Exception if param parsing failed
     */
    public Message convertToPBParam(HttpServletRequest request,
            Class<? extends Message> msgType) throws Exception {
        // For a GET request, the parameters are directly encapsulated into a Map before conversion.
        if (HttpConstants.HTTP_METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> params = getRequestParams(request);
            return convertParamToPBParam(msgType, params);
        }

        checkRequestMethod(request, HttpConstants.HTTP_METHOD_POST);

        String contentType = request.getContentType().toLowerCase();
        if (contentType.startsWith(HttpConstants.CONTENT_TYPE_JSON)) {
            int contentLength = request.getContentLength();
            byte[] contentBytes = StreamUtils.readAllBytes(request.getInputStream(), contentLength);

            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class)
                    .getExtension(JSONSerialization.NAME);
            return serialization.deserialize(contentBytes, msgType);
        }

        if (contentType.equalsIgnoreCase(HttpConstants.CONTENT_TYPE_PROTOBUF)) {
            return convertToPBParamWithPBType(request, msgType);
        }
        throw new IllegalArgumentException("unsupported content-type " + contentType);
    }

    /**
     * Convert map params to a pb param
     *
     * @param msgType the type of decoded param
     * @param params origin map params
     * @return decoded param
     * @throws Exception if param parsing failed
     */
    private Message convertParamToPBParam(Class<? extends Message> msgType,
            Map<String, Object> params) throws Exception {
        Method getDefaultInstance = msgType.getDeclaredMethod("getDefaultInstance");
        Message pbMsg = (Message) getDefaultInstance.invoke(null);
        return ProtoJsonConverter.mapToMessage(params, pbMsg);
    }

    /**
     * Convert HTTP params to Map params
     *
     * @param request HttpServletRequest
     * @return map params
     */
    private Map<String, Object> getRequestParams(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        Map<String, Object> params = new HashMap<>();
        while (parameterNames.hasMoreElements()) {
            String param = parameterNames.nextElement();
            params.put(param, request.getParameter(param));
        }
        return params;
    }

    /**
     * Check request method, only used by SpringMVC.
     *
     * @param request HttpServletRequest
     * @param expectMethod the expected HTTP request method
     */
    protected void checkRequestMethod(HttpServletRequest request, String expectMethod) {
    }

    /**
     * Deserialize request body to a PB message
     *
     * @param request HttpServletRequest
     * @param msgType the type of decoded param
     * @return decoded param
     * @throws Exception if param parsing failed
     */
    protected Message convertToPBParamWithPBType(HttpServletRequest request,
            Class<? extends Message> msgType) throws Exception {
        int contentLength = request.getContentLength();
        byte[] contentBytes = StreamUtils.readAllBytes(request.getInputStream(), contentLength);

        Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class)
                .getExtension(PBSerialization.NAME);
        return serialization.deserialize(contentBytes, msgType);
    }

    /**
     * Convert HTTP request body to a JSON map
     *
     * @param request HttpServletRequest
     * @return map params
     * @throws Exception if param parsing failed
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToJsonParam(HttpServletRequest request) throws Exception {
        // For a GET request, the parameters are directly encapsulated into a Map before conversion.
        if (HttpConstants.HTTP_METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            return getRequestParams(request);
        }
        checkRequestMethod(request, HttpConstants.HTTP_METHOD_POST);
        String contentType = request.getContentType().toLowerCase();
        if (contentType.startsWith(HttpConstants.CONTENT_TYPE_JSON)) {
            int contentLength = request.getContentLength();
            byte[] contentBytes = StreamUtils.readAllBytes(request.getInputStream(), contentLength);

            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class)
                    .getExtension(JSONSerialization.NAME);
            return (Map<String, Object>) serialization.deserialize(contentBytes, Map.class);
        } else { // only support application/json
            throw new IllegalArgumentException("unsupported content-type " + contentType);
        }
    }

    /**
     * Convert HTTP request body to POJO
     *
     * @param request HttpServletRequest
     * @param msgType the type of decoded param
     * @return decoded param
     * @throws Exception if param parsing failed
     */
    public Object convertToJavaBean(HttpServletRequest request, Class<?> msgType) throws Exception {
        Object bean = msgType.newInstance();
        if (HttpConstants.HTTP_METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            Map<String, String[]> requestParameterMap = request.getParameterMap();
            BeanUtils.populate(bean, requestParameterMap);
            return bean;
        }
        String contentType = request.getContentType().toLowerCase();
        if (contentType.startsWith(HttpConstants.CONTENT_TYPE_JSON)) {
            int contentLength = request.getContentLength();
            byte[] contentBytes = StreamUtils.readAllBytes(request.getInputStream(), contentLength);

            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class)
                    .getExtension(JSONSerialization.NAME);
            bean = serialization.deserialize(contentBytes, msgType);
        }
        return bean;
    }

    /**
     * Serialize the return result. Currently, all results are converted to JSON.
     *
     * @param response HttpServletResponse
     * @param result rpc response
     * @throws Exception if write http response failed
     */
    public void writeHttpResponse(HttpServletResponse response, Response result) throws Exception {
        Object value = result.getValue();

        byte[] data = null;
        if (value instanceof Message) {
            Map<String, Object> jsonData = ProtoJsonConverter.messageToMap((Message) value);
            Serialization jsonSerialization =
                    ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(JSONSerialization.NAME);
            data = jsonSerialization.serialize(jsonData);
        } else if (value != null) {
            if (value instanceof String) {
                data = ((String) value).getBytes();
            } else {
                Serialization jsonSerialization =
                        ExtensionLoader.getExtensionLoader(Serialization.class)
                                .getExtension(JSONSerialization.NAME);
                data = jsonSerialization.serialize(value);
            }
        }

        if (data != null) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
            response.setContentLength(data.length);
            ServletOutputStream os = response.getOutputStream();
            os.write(data);
        }
    }

}
