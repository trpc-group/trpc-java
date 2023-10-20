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

package com.tencent.trpc.admin.dto;

import java.util.List;

/**
 * Admin command view class
 */
public class CommandDto extends CommonDto {

    /**
     * Command list
     */
    private List<String> cmds;

    public CommandDto() {
    }

    public CommandDto(List<String> cmds) {
        this.cmds = cmds;
    }

    public List<String> getCmds() {
        return cmds;
    }

    public void setCmds(List<String> cmds) {
        this.cmds = cmds;
    }

    @Override
    public String toString() {
        return "CommandDto{" + "cmds=" + cmds + "} " + super.toString();
    }
}
