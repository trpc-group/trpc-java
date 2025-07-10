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

package com.tencent.trpc.core.serialization.support;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.serialization.SerializationType;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.serialization.support.helper.ProtoCodecManager;

@Extension(JavaPBSerialization.NAME)
public class JavaPBSerialization implements Serialization {

    public static final String NAME = "jpb";

    @Override
    public byte[] serialize(Object obj) {
        try {
            Codec codec = ProtoCodecManager.getCodec(obj.getClass());
            return codec.encode(obj);
        } catch (Exception e) {
            throw TRpcException
                    .newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) {
        T obj;
        try {
            Codec<T> codec = ProtoCodecManager.getCodec(clz);
            obj = codec.decode(data);
        } catch (Exception e) {
            throw TRpcException
                    .newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, e.getMessage(), e);
        }
        return obj;
    }

    @Override
    public int type() {
        // Since the content of Java PB and PB serialization is consistent, when using Java PB serialization,
        // in order to communicate with other languages, we set the serialization method in the protocol to PB.
        // When deserializing, compatibility processing will be done in the PB deserialization protocol.
        return SerializationType.PB;
    }

    @Override
    public String name() {
        return NAME;
    }
    
}
