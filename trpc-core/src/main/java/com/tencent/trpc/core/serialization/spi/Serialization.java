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

package com.tencent.trpc.core.serialization.spi;

import com.tencent.trpc.core.extension.Extensible;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Serialization SPI.
 */
@Extensible("pb")
public interface Serialization {

    /**
     * Serialization interface.
     *
     * @param obj the object to be serialized
     * @return the serialized byte array
     * @throws IOException IO exception
     */
    byte[] serialize(Object obj) throws IOException;

    /**
     * Deserialize byte array into an object, does not support generics.
     *
     * @param bytes the byte array to be deserialized
     * @param clz the object type after deserialization
     * @param <T> the instance type after deserialization
     * @return the instance after deserialization
     * @throws IOException IO exception
     */
    <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException;

    /**
     * Deserialize byte array into an object, supports JSON deserialization generics.
     *
     * <p>The default implementation does not support generics, only the Json serialization implementation needs to
     * override this method.</p>
     *
     * @param bytes the byte array to be deserialized
     * @param type the original object type
     * @param <T> the generic type
     * @return the object after deserialization
     * @throws IOException io exception
     */
    default <T> T deserialize(byte[] bytes, Type type) throws IOException {
        return deserialize(bytes, (Class<T>) type);
    }

    /**
     * Framework usage: 0-127.
     */
    int type();

    String name();

}
