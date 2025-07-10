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

package com.tencent.trpc.springmvc.proto;

import com.google.protobuf.Message;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using {@link ProtoJsonConverter}.
 *
 * This converter can be used to bind to {@link com.google.protobuf.Message}.
 * By default, it supports {@code application/json} with {@code UTF-8} character set.
 *
 * @see com.tencent.trpc.core.utils.ProtoJsonConverter
 */
public class ProtoJsonHttpMessageConverter<T extends Message> extends AbstractHttpMessageConverter<T> implements
        Ordered {

    public ProtoJsonHttpMessageConverter() {
        this(StandardCharsets.UTF_8);
    }

    public ProtoJsonHttpMessageConverter(Charset charset) {
        super(MediaType.APPLICATION_JSON);
        setDefaultCharset(Objects.requireNonNull(charset, "charset must not be null"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Message.class.isAssignableFrom(clazz);
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return ProtoJsonConverter.jsonToMessage(IOUtils.toString(inputMessage.getBody(), getDefaultCharset()), clazz);
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(ProtoJsonConverter.messageToJson(t, false, false).getBytes(getDefaultCharset()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
