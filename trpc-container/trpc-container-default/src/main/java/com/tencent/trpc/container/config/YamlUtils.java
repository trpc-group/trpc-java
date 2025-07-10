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

package com.tencent.trpc.container.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class YamlUtils {

    private final String position;

    public YamlUtils(String position) {
        this.position = position;
    }

    public String getString(Map<String, Object> yamlMapConfig, String key) {
        String result = StringUtils.trim(MapUtils.getString(yamlMapConfig, key));
        if (result == null) {
            throwException(position, key, "value is null");
        }
        return result;
    }

    public int getInteger(Map<String, Object> yamlMapConfig, String key) {
        Integer result = MapUtils.getInteger(yamlMapConfig, key);
        if (result == null) {
            throwException(position, key, "value is not integer");
        }
        return result;
    }

    public boolean getBoolean(Map<String, Object> yamlMapConfig, String key) {
        Boolean result = MapUtils.getBoolean(yamlMapConfig, key);
        if (result == null) {
            throwException(position, key, "value is not boolean");
        }
        return result;
    }

    public Collection getCollection(Map<String, Object> yamlMapConfig, String key) {
        if (!yamlMapConfig.containsKey(key)) {
            return Collections.EMPTY_LIST;
        }
        Object obj = yamlMapConfig.get(key);
        if (!(obj instanceof Collection)) {
            throwException(position, key, "value is not collection");
        }
        return (Collection) obj;
    }

    public List<String> getStringList(Map<String, Object> yamlMapConfig, String key) {
        return (List<String>) getCollection(yamlMapConfig, key);
    }

    public List getList(Map<String, Object> yamlMapConfig, String key) {
        return (List) getCollection(yamlMapConfig, key);
    }

    public Map<String, Object> getMap(Map<String, Object> yamlMapConfig, String key) {
        if (!yamlMapConfig.containsKey(key)) {
            return Collections.emptyMap();
        }
        Object obj = yamlMapConfig.get(key);
        if (!(obj instanceof Map)) {
            throwException(position, key, "value is not collection");
        }
        return (Map<String, Object>) obj;
    }

    public Map<String, Object> requireMap(Object obj, String key) {
        if (!(obj instanceof Map) || MapUtils.isEmpty((Map) obj)) {
            throwException(position, key, "value is not map");
        }
        return (Map<String, Object>) obj;
    }

    private void throwException(String position, String key, String casuse) {
        throw new IllegalArgumentException("Yaml parser exception, position at (" + position
                + "), key (" + key + "), cause: " + casuse);
    }

}
