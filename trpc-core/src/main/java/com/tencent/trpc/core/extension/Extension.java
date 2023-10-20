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

package com.tencent.trpc.core.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * Can be used to mark the plugin implementation class and configure the name and other properties of the plugin.
 * When the plugin implementation class does not mark this annotation, all properties use default values.
 * note: 1）All plugins are configured in the corresponding configuration in the spi directory for easy loading
 *          when the framework starts.
 *       2）Each plugin name object will instantiate a plugin object,
 *          and different plugin name objects correspond to different plugin objects.
 *       3）Each type of plugin should be designed in a multi-instance manner.
 * </pre>
 *
 * @see Extensible
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {

    /**
     * Indicates the plugin name, which is empty to indicate that the plugin name is parsed from the configuration file.
     * It supports configuring the plugin name in both the configuration file and this annotation,
     * and the name configured in the configuration file is used first.
     *
     * @return String default ""
     */
    String value() default "";

    /**
     * Indicates the plugin order, and the larger the value, the lower the priority.
     *
     * @return int default 0
     */
    int order() default 0;
    
}