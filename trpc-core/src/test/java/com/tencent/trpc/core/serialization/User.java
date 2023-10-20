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

package com.tencent.trpc.core.serialization;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import java.util.Map;

public class User {

    @Protobuf(order = 1)
    private String name;
    @Protobuf(order = 2)
    private String desc;
    @Protobuf(order = 3)
    private Map<String, String> members;

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Map<String, String> getMembers() {
        return members;
    }

    public void setMembers(Map<String, String> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "User{" + "name='" + name + '\'' + ", desc='" + desc + '\'' + ", members=" + members + '}';
    }
}