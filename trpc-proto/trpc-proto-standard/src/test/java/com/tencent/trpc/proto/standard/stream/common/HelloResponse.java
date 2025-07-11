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

package com.tencent.trpc.proto.standard.stream.common;

import com.tencent.trpc.core.serialization.support.helper.annotation.Tag;
import java.util.Map;
import java.util.StringJoiner;

public class HelloResponse {

    @Tag(1)
    private String message;
    @Tag(2)
    private Integer count;
    @Tag(3)
    private Map<String, String> ext;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Map<String, String> getExt() {
        return ext;
    }

    public void setExt(Map<String, String> ext) {
        this.ext = ext;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HelloResponse.class.getSimpleName() + "[", "]")
                .add("message='" + message + "'")
                .add("count=" + count)
                .add("ext=" + ext)
                .toString();
    }
}