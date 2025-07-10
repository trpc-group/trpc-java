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

package com.tencent.trpc.core.sign.spi;

import com.tencent.trpc.core.extension.Extensible;
import java.util.Objects;

/**
 * Sign the request body data.
 */
@Extensible
public interface Sign {

    /**
     * Signature name.
     *
     * @return the implementation name of the digital signature
     */
    String name();

    /**
     * Calculate the signature digest.
     *
     * @param body the message body to be sent
     * @return the calculated digest
     */
    String digest(byte[] body);

    /**
     * Verify the signature.
     *
     * @param body the body to be verified
     * @param signature the signature
     * @return true if the verification is successful, otherwise return false
     */
    default boolean verify(byte[] body, String signature) {
        return Objects.equals(digest(body), signature);
    }

}
