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

package com.tencent.trpc.proto.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.transport.netty.NettyClientTransportFactory;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.junit.Test;

public class DefResponseFutureTest {

    @Test
    public void test() throws Exception {
        ProtocolConfig config = ProtocolConfig.newInstance();
        config.setIp("127.0.0.1");
        config.setPort(8888);
        DefRpcClient rpcClient = new DefRpcClient(config, new TestClientCodec());
        ConsumerInvoker invoker = new DefConsumerInvoker(rpcClient, new ConsumerConfig<>());
        ClientTransport client = new NettyClientTransportFactory().create(config,
                new ChannelHandlerAdapter() {
                }, new TestClientCodec());
        DefRequest request = new DefRequest();
        request.setRequestId(1000);
        request.getMeta().setTimeout(1000);
        RpcClientContext context = new RpcClientContext();
        DefResponseFuture future = new DefResponseFuture(context, invoker, client, request);
        DefResponseFutureManager manager = new DefResponseFutureManager();
        manager.newFuture(context, invoker, client, request);
        assertEquals(future.getContext(), context);
        assertEquals(future.getInvoker(), invoker);
        assertEquals(future.getRequest(), request);
        assertEquals(future.getTimeout(), 1000);
        future.setClient(client);
        future.setContext(context);
        future.setInvoker(invoker);
        future.setRequest(request);
        assertEquals(future.getContext(), context);
        assertEquals(future.getInvoker(), invoker);
        assertEquals(future.getRequest(), request);
        assertEquals(future.getTimeout(), 1000);

        Exception ex = null;
        try {
            manager.newFuture(context, invoker, client, request);
        } catch (Exception e) {
            ex = e;
        }
        assertEquals(((TRpcException) ex).getCode(), ErrorCode.TRPC_INVOKE_UNKNOWN_ERR);

        try {
            future.get();
        } catch (Exception e) {
            ex = e;
        }
        assertEquals(((TRpcException) ex).getCode(), ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR);

        DefResponse value = new DefResponse();
        future.complete(value);

        assertEquals(future.get(), value);
        rpcClient.close();
        client.close();
        future.getRpcMethodInfo();
        manager.closeClient(client);

        Field f = manager.getClass().getDeclaredField("futureMap");
        f.setAccessible(true);
        ConcurrentMap<Long, DefResponseFuture> map =
                (ConcurrentMap<Long, DefResponseFuture>) f.get(manager);
        assertTrue(map.size() == 0);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        manager.stop();
    }

    private class TestClientCodec extends ClientCodec {

        @Override
        public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {
        }

        @Override
        public Object decode(Channel channel, ChannelBuffer channelBuffer) {
            return null;
        }

    }

}
