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

package com.tencent.trpc.core.exception;

import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionException;

public class ExceptionHelper {

    /**
     * Parse the whenComplete exception to get the response or process exception.
     *
     * <pre>
     * CompletableFuture.whenComplete((response, throwable) -> {
     *     Throwable e = ExceptionHelper.parseCompletionException(response, throwable);
     * }
     * </pre>
     *
     * @param response the response
     * @param throwable the throwable
     * @return the parsed exception
     */
    public static Throwable parseResponseException(Response response, Throwable throwable) {
        if (throwable != null) {
            return ExceptionHelper.unwrapCompletionException(throwable);
        } else if (response != null) {
            Throwable exception = response.getException();
            if (exception != null) {
                return ExceptionHelper.unwrapCompletionException(exception);
            }
        }
        return null;
    }

    /**
     * Check if the exception is a TRpc exception.
     *
     * @param throwable the throwable
     * @return true if it's a TRpc exception, false otherwise
     */
    public static boolean isTRpcException(Throwable throwable) {
        return throwable instanceof TRpcException;
    }

    /**
     * Unwrap the exception.
     *
     * @param throwable the exception
     * @return the unwrapped exception
     */
    public static Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof CompletionException) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Check if the exception is a business exception.
     *
     * @param throwable the exception
     * @return true if it's a business exception, false otherwise
     */
    public static boolean isBizException(Throwable throwable) {
        if (!(throwable instanceof TRpcException)) {
            return false;
        }
        return ((TRpcException) throwable).isBizException();
    }

}
