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

package com.tencent.trpc.core.utils;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON and Protocol Buffers conversion utility.
 */
@SuppressWarnings("unchecked")
public class ProtoJsonConverter {

    private static final Logger LOG = LoggerFactory.getLogger(ProtoJsonConverter.class);

    private static final Map<Class<? extends Message>, Builder> BUILDER_CACHE = new ConcurrentHashMap<>(64);

    private static <R extends Builder, T extends Message> R newBuilder(Class<T> clazz) {
        Objects.requireNonNull(clazz, "protobuf message class must not be null");
        return (R) BUILDER_CACHE.computeIfAbsent(clazz, c -> {
            try {
                return (Builder) c.getMethod("newBuilder").invoke(null);
            } catch (Exception e) {
                throw new IllegalStateException("newBuilder for class '" + clazz + "' error", e);
            }
        }).getDefaultInstanceForType().newBuilderForType();
    }

    /**
     * Used only for human-readable logging, not for logical processing.
     *
     * @param value the object to be printed
     * @return the resulting string
     */
    public static String toString(Object value) {
        return toString(value, false);
    }

    /**
     * toString method, used only for human-readable logging.
     *
     * @param value the object to be printed
     * @param throwException whether to throw an exception
     * @return the resulting string
     */
    public static String toString(Object value, boolean throwException) {
        try {
            if (value == null) {
                return null;
            }
            if (value instanceof Message) {
                return ProtoJsonConverter.messageToJson((Message) value);
            }
            return value.toString();
        } catch (Exception ex) {
            if (throwException) {
                throw new RuntimeException(ex);
            } else {
                LOG.error("toString exception", ex);
                return ex.toString();
            }
        }
    }

    /**
     * Convert Protocol Buffers message to a map.
     *
     * @param message the source Protocol Buffers message
     * @return the resulting map
     */
    public static Map<String, Object> messageToMap(Message message) {
        try {
            Objects.requireNonNull(message, "message");
            return JsonUtils.fromJson(
                    JsonFormat.printer().printingEnumsAsInts().includingDefaultValueFields()
                            .print(message),
                    Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("pb message to map exception:", ex);
        }
    }

    /**
     * Convert a map to a Protocol Buffers message.
     *
     * @param map the source map
     * @param message the Protocol Buffers message object instance
     * @return the resulting Protocol Buffers message
     */
    public static Message mapToMessage(Map<String, Object> map, Message message) {
        try {
            Objects.requireNonNull(map, "map");
            Objects.requireNonNull(message, "message");
            return jsonToMessage(JsonUtils.toJson(map), message);
        } catch (Exception ex) {
            throw new RuntimeException("map to pb message exception:", ex);
        }
    }

    /**
     * Convert a map to a Protocol Buffers builder.
     *
     * @param map the source map
     * @param builder the target Protocol Buffers builder object instance
     */
    public static void mapToBuilder(Map<String, Object> map, Builder builder) {
        try {
            Objects.requireNonNull(map, "map");
            Objects.requireNonNull(builder, "builder");
            JsonFormat.parser().ignoringUnknownFields()
                    .merge(JsonUtils.toJson(map), builder);
        } catch (Exception ex) {
            throw new RuntimeException("map to pb builder exception:", ex);
        }
    }

    /**
     * Convert a Protocol Buffers message to JSON.
     *
     * @param message the source Protocol Buffers message
     * @return the resulting JSON string
     */
    public static String messageToJson(Message message) {
        return messageToJson(message, true, true);
    }

    /**
     * Convert a Protocol Buffers message to JSON.
     *
     * @param message the source of pb message
     * @param pretty whether to output json that fits in a page for pretty printing
     * @param includingDefaultValueFields whether to print fields set to their defaults
     * @return the result of json string
     */
    public static String messageToJson(Message message, boolean pretty, boolean includingDefaultValueFields) {
        try {
            Objects.requireNonNull(message, "message");
            return newJsonFormatPrinter(pretty, includingDefaultValueFields).print(message);
        } catch (Exception ex) {
            throw new RuntimeException("pb message to json exception:", ex);
        }
    }

    /**
     * Convert a JSON string to a Protocol Buffers message.
     *
     * @param json the source JSON string
     * @param message the Protocol Buffers message object instance
     * @return the resulting Protocol Buffers message
     */
    public static Message jsonToMessage(String json, Message message) {
        try {
            Objects.requireNonNull(json, "json");
            Objects.requireNonNull(message, "message");
            Builder builder = message.toBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
            return builder.build();
        } catch (Exception ex) {
            throw new RuntimeException("json to pb message exception:", ex);
        }
    }

    /**
     * The json string to pb.
     *
     * @param json the source of json string
     * @param clazz the pb message type
     * @return the result of pb message
     */
    public static <T extends Message> T jsonToMessage(String json, Class<T> clazz) {
        Message message = newBuilder((Class<Message>) clazz).build();
        return (T) jsonToMessage(json, message);
    }

    /**
     * New JsonFormat Printer.
     *
     * @param pretty whether to output json that fits in a page for pretty printing
     * @param includingDefaultValueFields whether to print fields set to their defaults
     * @return the printer converts the protobuf message to json
     */
    private static Printer newJsonFormatPrinter(boolean pretty, boolean includingDefaultValueFields) {
        Printer printer = JsonFormat.printer().printingEnumsAsInts();
        if (!pretty) {
            printer = printer.omittingInsignificantWhitespace();
        }
        if (includingDefaultValueFields) {
            printer = printer.includingDefaultValueFields();
        }
        return printer;
    }

}
