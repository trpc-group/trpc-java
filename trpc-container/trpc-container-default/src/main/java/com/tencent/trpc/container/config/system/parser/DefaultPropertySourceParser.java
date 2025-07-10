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

package com.tencent.trpc.container.config.system.parser;

import com.google.common.base.CharMatcher;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.list.GrowthList;
import org.apache.commons.lang.StringUtils;

/**
 * Default property source parser.
 *
 * <p>Convert a hierarchical map to a flattened map structure (supports multi-level structure, list,
 * and nested types in the list).</p>
 * {@code
 * Input:
 * {
 * "global":{
 * "namespace":"${env_type}"
 * }
 * }
 * Output:
 * {
 * "global.namespace":"${env_type}"
 * }
 * }
 *
 * <p>Convert a flattened map structure to a hierarchical map structure (reverse logic of the above Input, Output).</p>
 */
@Extension("default")
public class DefaultPropertySourceParser implements PropertySourceParser {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPropertySourceParser.class);

    /**
     * Represents an element.
     */
    private static final int SINGLE_ELEMENT = 1;
    /**
     * Default capacity of the map.
     */
    private static final int DEFAULT_CAPACITY = 16;

    @Override
    public Map<String, Object> getFlattableMap(Map<String, Object> source) {
        Objects.requireNonNull(source, "source map can't be null");
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    @Override
    public Map<String, Object> parseFlattableMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object attrKey : source.keySet()) {
            try {
                parseMultiLayerMap(result, (String) attrKey, source.get(attrKey));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "parse source map failed, error message is: [" + e.getMessage() + "]");
            }
        }
        return result;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        source.forEach((key, value) -> {
            if (StringUtils.isNotBlank(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                // need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                // need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                if (collection.isEmpty()) {
                    result.put(key, "");
                } else {
                    int count = 0;
                    for (Object object : collection) {
                        buildFlattenedMap(result, Collections.singletonMap(
                                "[" + (count++) + "]", object), key);
                    }
                }
            } else {
                result.put(key, (value != null ? value : ""));
            }
        });
    }

    private void parseMultiLayerMap(Map<String, Object> result, String attrKey, Object attrValue) {
        if (StringUtils.isBlank(attrKey)
                || null == attrValue) {
            return;
        }
        String[] attrs = StringUtils.splitByWholeSeparator(attrKey, ".");
        String firstAttr = attrs[0];
        // For properties separated by '.', if the last character is "]", it represents a list
        if (firstAttr.endsWith("]")) {
            List<Object> childList = newChildList(result, listAttr(firstAttr));
            parseMultiLayerList(childList, lastPath(attrKey, firstAttr), attrValue);
        } else {
            if (attrs.length == SINGLE_ELEMENT) {
                result.put(firstAttr, attrValue);
            } else {
                Map<String, Object> childMap = newChildMap(result, firstAttr);
                parseMultiLayerMap(childMap, nextAttrAfterPoint(attrKey), attrValue);
            }
        }
    }

    private void parseMultiLayerList(List<Object> parent, String lastPath, Object attrValue) {
        if (StringUtils.isBlank(lastPath)
                || null == attrValue) {
            return;
        }
        //[0].name
        String[] attrs = StringUtils.splitByWholeSeparator(lastPath, ".");
        //[0] -> 0
        int index = Integer.parseInt(CharMatcher.digit().retainFrom(attrs[0]));
        if (attrs.length == SINGLE_ELEMENT) {
            parent.add(index, attrValue);
        } else {
            Map<String, Object> childMap = newChildMapWithList(parent, index);
            parseMultiLayerMap(childMap, nextAttrAfterPoint(lastPath), attrValue);
        }
    }

    private Map<String, Object> newChildMap(Map<String, Object> result, String attrKey) {
        Object attrValue = result.get(attrKey);
        if (attrValue != null) {
            if (attrValue instanceof Map) {
                return (Map<String, Object>) attrValue;
            }
            return new HashMap<>();
        } else {
            result.put(attrKey, new LinkedHashMap<>(DEFAULT_CAPACITY));
            return (Map<String, Object>) result.get(attrKey);
        }
    }

    private Map<String, Object> newChildMapWithList(List<Object> parentList, int index) {
        try {
            Object element = parentList.get(index);
            if (null != element) {
                return (Map<String, Object>) element;
            }
        } catch (Exception e) {
            logger.warn("parent list is null, error: {}", e);
        }

        //use set(), to prevent null values
        parentList.set(index, new LinkedHashMap<>(DEFAULT_CAPACITY));
        return (Map<String, Object>) parentList.get(index);
    }

    private List<Object> newChildList(Map<String, Object> result, String attrKey) {
        if (!result.containsKey(attrKey)) {
            result.put(attrKey, new GrowthList(DEFAULT_CAPACITY));
        }
        return (List<Object>) result.get(attrKey);
    }

    /**
     * [1].name to name
     *
     * @param attrKey key
     * @return attributes name
     */
    private String nextAttrAfterPoint(String attrKey) {
        int index = attrKey.indexOf(".");
        if (index != -1) {
            return attrKey.substring(index + SINGLE_ELEMENT);
        }
        return "";
    }

    /**
     * service[0].name to service
     *
     * @param firstAttr key
     * @return service name
     */
    private String listAttr(String firstAttr) {
        int index = firstAttr.indexOf("[");
        if (index != -1) {
            return firstAttr.substring(0, index);
        }
        return "";
    }

    /**
     * service[0].name to [0].name
     *
     * @param attrKey key
     * @param firstAttr prefix key
     * @return remove the key of the attribute name
     */
    private String lastPath(String attrKey, String firstAttr) {
        int index = firstAttr.indexOf("[");
        if (index != -1) {
            return attrKey.substring(index);
        }
        return "";
    }
}
