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

package com.tencent.trpc.core.trace;

import java.nio.charset.Charset;

public interface TracerConstants {

    Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    int CUSTOM_EXCEPTION_CODE = -99999;

    interface Keys {

        String TRACE_ERROR_KEY = "_trace_ext.trace.error_flag";
        String RESULT_CODE = "_trace_ext.ret";
        String DYEING_KEY = "_trace_ext.dyeing_key";
        String LOCAL_HOSTNAME = "_trace_ext.hostname";
        String TRACE_ERROR_VALUE = "1";
    }

}
