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

package com.tencent.trpc.admin.impl;

import com.tencent.trpc.admin.dto.CommandDto;
import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Command Operation Management
 */
@Path("/cmds")
public class CommandAdmin implements Admin {

    /**
     * Command List
     *
     * @return CommandDto
     */
    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public CommandDto getCommands() {
        List<String> commands = new ArrayList<>();
        Collection<ExtensionClass<Admin>> extensionClasses = ExtensionLoader
                .getExtensionLoader(Admin.class).getAllExtensionClass();
        for (ExtensionClass<Admin> extensionClass : extensionClasses) {
            Class clazz = extensionClass.getClazz();
            Annotation annotation = clazz.getAnnotation(Path.class);
            if (annotation != null) {
                Path classPath = (Path) annotation;
                String classUrl = classPath.value();
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    Path methodPath = method.getAnnotation(Path.class);
                    if (methodPath != null) {
                        commands.add(classUrl + methodPath.value());
                    }
                }
            }
        }
        return new CommandDto(commands);
    }

}
