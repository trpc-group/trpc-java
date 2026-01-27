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

package com.tencent.trpc.transport.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.utils.FutureUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NettyFutureUtilsTest {

    private static void checkChannelInManager(TestChannel channel) {
        ConcurrentMap<Channel, NettyChannel> channelmap = NettyChannelManager.getChannelMap();
        assertTrue(((NettyChannel) (channelmap.values().toArray()[0])).getIoChannel() == channel);
    }

    @BeforeEach
    public void before() {
        NettyChannelManager.getChannelMap().clear();
    }

    /**
     * future.isSuccess()
     */
    @Test
    public void futureisSuccessTest() {
        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setDone(true);
        future.setSuccess(true);
        future.setChannel(channel);
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        ProtocolConfig config = new ProtocolConfig();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        checkChannelInManager(channel);
        Assertions.assertTrue(((NettyChannel) newFuture.join()).getIoChannel() == channel);
    }

    /**
     * future.isCancelled()
     */
    @Test
    public void futureIsCancelTest() {
        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setCancle(true);
        future.setSuccess(false);
        future.setChannel(channel);
        future.setCause(new RuntimeException(""));
        ProtocolConfig config = new ProtocolConfig();
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        sleep(1);
        assertEquals(NettyChannelManager.getChannelMap().keySet().size(), 0);
        assertTrue(newFuture.isCancelled());
    }

    /**
     * future.isCancelled()
     */
    @Test
    public void futureIsDoneAndNotSuccessTest() {

        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setDone(true);
        future.setCancle(false);
        future.setSuccess(false);
        future.setChannel(channel);
        future.setCause(new RuntimeException("abc"));
        ProtocolConfig config = new ProtocolConfig();
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        assertEquals(NettyChannelManager.getChannelMap().keySet().size(), 0);
        Exception ex = null;
        try {
            newFuture.get();
        } catch (Exception e) {
            ex = e;
        }
        assertTrue(ex.getCause() instanceof RuntimeException
                && ex.getCause().getMessage().contentEquals("abc"));
    }

    /**
     * future.addListener(f ->
     */
    @Test
    public void futureListenerAndSuccess() {

        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setDone(false);
        future.setCancle(false);
        future.setSuccess(false);
        future.setChannel(channel);
        ChannelTestFuture notifyfuture = new ChannelTestFuture();
        future.setNotify(notifyfuture);
        notifyfuture.setSuccess(true);
        ProtocolConfig config = new ProtocolConfig();
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        assertEquals(NettyChannelManager.getChannelMap().keySet().size(), 1);
        Exception ex = null;
    }

    @Test
    public void futureListenerAndNotSuccess() {

        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setDone(false);
        future.setCancle(false);
        future.setSuccess(false);
        future.setChannel(channel);
        ChannelTestFuture notifyfuture = new ChannelTestFuture();
        future.setNotify(notifyfuture);
        notifyfuture.setSuccess(false);
        notifyfuture.setCause(new RuntimeException("abc"));
        ProtocolConfig config = new ProtocolConfig();
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        assertEquals(NettyChannelManager.getChannelMap().keySet().size(), 0);
        Exception ex = null;
        try {
            newFuture.get();
        } catch (Exception e) {
            ex = e;
        }
        assertTrue(ex.getCause() instanceof RuntimeException
                && ex.getCause().getMessage().contentEquals("abc"));
    }

    @Test
    public void futureListenerSuccessAndComplestageError() {

        TestChannel channel = new TestChannel();
        ChannelTestFuture future = new ChannelTestFuture();
        future.setDone(false);
        future.setCancle(false);
        future.setSuccess(false);
        future.setChannel(channel);
        ChannelTestFuture notifyfuture = new ChannelTestFuture();
        future.setNotify(notifyfuture);
        notifyfuture.setSuccess(false);
        notifyfuture.setCause(new RuntimeException("abc"));
        ProtocolConfig config = new ProtocolConfig();
        CompletableFuture<com.tencent.trpc.core.transport.Channel> newFuture = FutureUtils
                .newFuture();
        NettyFutureUtils.adaptConnectingFuture(future, newFuture, config);
        assertEquals(NettyChannelManager.getChannelMap().keySet().size(), 0);
        Exception ex = null;
        try {
            newFuture.get();
        } catch (Exception e) {
            ex = e;
        }
        assertTrue(ex.getCause() instanceof RuntimeException
                && ex.getCause().getMessage().contentEquals("abc"));
    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    private static class TestChannel extends NettyChannelTestAdaptor {


    }

    private static class ChannelTestFuture<T> extends NettyChannelFutureTestAdaptor<T> {

        private io.netty.channel.Channel channe;
        private boolean done;
        private boolean cancle;
        private boolean success;
        private Throwable cause;
        private Future future;
        private ChannelTestFuture notify;

        public ChannelTestFuture getNotify() {
            return notify;
        }

        public void setNotify(ChannelTestFuture notify) {
            this.notify = notify;
        }

        public boolean isCancelled() {
            return cancle;
        }

        public void setCancle(boolean cancle) {
            this.cancle = cancle;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public Channel channel() {
            return channe;
        }

        public Channel getChannel() {
            return channe;
        }

        public void setChannel(io.netty.channel.Channel channe) {
            this.channe = channe;
        }

        public Throwable cause() {
            return cause;
        }

        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public ChannelFuture addListener(
                GenericFutureListener<? extends Future<? super Void>> listener) {
            try {
                GenericFutureListener local = (GenericFutureListener) listener;
                local.operationComplete(notify);
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ;

        public void notifyListener(Future future) {
            this.future = future;
        }

    }
}
