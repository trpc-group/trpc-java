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

package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A netty tcp ClientTransport.
 * <p>Long-connection mode with read-idle close: when {@code config.idleTimeout > 0} an
 * {@link IdleStateHandler} is installed in <b>READ idle</b> mode so that a channel which
 * has not received any inbound bytes for {@code idleTimeout} ms is torn down on the
 * client side. The next request goes through
 * {@link com.tencent.trpc.core.transport.AbstractClientTransport#ensureChannelActive} and
 * rebuilds a fresh TCP connection on the existing {@code EventLoopGroup}.</p>
 * <p>READ idle (not ALL idle) is intentional: under "persistent traffic + silent packet
 * drop" half-dead scenarios the client keeps writing successfully into the kernel send
 * buffer, so {@code ALL_IDLE}/{@code WRITE_IDLE} would never fire — only the lack of any
 * inbound reply makes the dead path observable within {@code idleTimeout}.</p>
 * <p>TCP keepalive tuning: when {@code ioMode=epoll} on Linux (regardless of whether
 * {@code ioThreadGroupShare} is true or false), the transport uses
 * {@code EpollSocketChannel} backed by a per-variant shared or independent
 * {@code EpollEventLoopGroup} and sets {@code TCP_KEEPIDLE}, {@code TCP_KEEPINTVL} and
 * {@code TCP_KEEPCNT} per channel. With the Dubbo-style 30/10/3 defaults a half-dead
 * connection is reset by the kernel within ~60 s, an order of magnitude faster than
 * {@code idleTimeout} alone. This kicks in transparently and only on Linux + epoll;
 * everywhere else the read-idle handler remains the universal fallback.</p>
 * <p>Trade-off: pure one-way callers (request-only, no response) will see the channel
 * recycled every {@code idleTimeout} ms, effectively turning the connection into a
 * short-lived one. Such callers should configure {@code idleTimeout = 0} to disable the
 * handler.</p>
 * <p>Before the actual {@code ctx.close()}, the slot is invalidated via
 * {@link com.tencent.trpc.core.transport.AbstractClientTransport#invalidateChannel} so that a
 * concurrent request thread cannot route onto a channel that is in the middle of closing.</p>
 */
public class NettyTcpClientTransport extends NettyAbstractClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpClientTransport.class);

    public NettyTcpClientTransport(ProtocolConfig config, ChannelHandler handler, ClientCodec clientCodec) {
        super(config, handler, clientCodec, "Netty-ShareTcpClientWorker");
    }

    @Override
    protected void doOpen() {
        bootstrap = new Bootstrap();
        // Decide the IO model: epoll if (a) the JVM has a working netty-epoll native
        // library, AND (b) the user opted into ioMode=epoll. The flag is independent of
        // ioThreadGroupShare — both shared and independent pools support epoll now.
        boolean useEpoll = wantsEpoll(config);
        EventLoopGroup myEventLoopGroup;
        Class<? extends io.netty.channel.Channel> channelClass = useEpoll
                ? EpollSocketChannel.class
                : NioSocketChannel.class;
        if (Boolean.TRUE.equals(config.isIoThreadGroupShare())) {
            // Reference counter has already been incremented in the constructor; just use
            // the shared group of the matching variant here. This avoids any TOCTOU race
            // between constructor and doOpen.
            myEventLoopGroup = getSharedEventLoopGroup();
        } else if (useEpoll) {
            myEventLoopGroup = new EpollEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-EpollTcpClientWorker-" + config.getIp() + ":" + config.getPort()));
        } else {
            myEventLoopGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-TcpClientWorker-" + config.getIp() + ":" + config.getPort()));
        }
        bootstrap.group(myEventLoopGroup).channel(channelClass)
                .option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnTimeout());
        if (config.getReceiveBuffer() > 0) {
            bootstrap.option(ChannelOption.SO_RCVBUF, config.getReceiveBuffer());
        }
        if (config.getSendBuffer() > 0) {
            bootstrap.option(ChannelOption.SO_SNDBUF, config.getSendBuffer());
        }
        if (useEpoll) {
            applyTcpKeepAliveTuning(bootstrap);
        }
        final NettyClientHandler clientHandler =
                new NettyClientHandler(getChannelHandler(), config, true);
        channelSet = clientHandler.getChannelSet();
        final long idleTimeoutMills = resolveIdleTimeoutMills();
        bootstrap.handler(new ChannelInitializer<io.netty.channel.Channel>() {
            @Override
            protected void initChannel(io.netty.channel.Channel ch) {
                ChannelPipeline p = ch.pipeline();
                if (codec == null) {
                    p.addLast("handler", clientHandler);
                } else {
                    NettyCodecAdapter nettyCodec = NettyCodecAdapter
                            .createTcpCodecAdapter(codec, config);
                    p.addLast("encode", nettyCodec.getEncoder())
                            .addLast("decode", nettyCodec.getDecoder())
                            .addLast("handler", clientHandler);
                }
                if (idleTimeoutMills > 0) {
                    // READ idle (not ALL idle): trigger when no inbound bytes have been
                    // observed for {@code idleTimeoutMills}, regardless of whether the
                    // application keeps writing. This is the critical knob for half-dead
                    // connection detection on platforms where TCP keepalive tuning is not
                    // available; on Linux + epoll the keepalive parameters above kick in
                    // first and recover the connection in seconds rather than minutes.
                    //
                    // The idle handlers MUST sit before {@code "handler"} (NettyClientHandler)
                    // because the latter does not propagate {@code channelActive} downstream,
                    // and IdleStateHandler relies on channelActive (or handlerAdded while
                    // active) to start its timer.
                    p.addBefore("handler", "idleState",
                            new IdleStateHandler(idleTimeoutMills, 0L, 0L, TimeUnit.MILLISECONDS));
                    p.addBefore("handler", "idleClose", new IdleCloseHandler());
                }
            }
        });
    }

    @Override
    protected CompletableFuture<com.tencent.trpc.core.transport.Channel> make() {
        return NettyFutureUtils.fromConnectingFuture(bootstrap.connect(getRemoteAddress()), config);
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    protected boolean useChannelPool() {
        return config.isKeepAlive();
    }

    /**
     * Apply Linux {@code TCP_KEEPIDLE / TCP_KEEPINTVL / TCP_KEEPCNT} to the bootstrap. A
     * non-positive configured value (or {@code null}) leaves the corresponding kernel
     * default in place. The whole tuning is silently no-op on non-Linux platforms; the
     * caller has already verified epoll availability before calling this method.
     */
    private void applyTcpKeepAliveTuning(Bootstrap bootstrap) {
        Integer idle = config.getTcpKeepAliveIdle();
        Integer intvl = config.getTcpKeepAliveIntvl();
        Integer cnt = config.getTcpKeepAliveCnt();
        if (idle != null && idle > 0) {
            bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, idle);
        }
        if (intvl != null && intvl > 0) {
            bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, intvl);
        }
        if (cnt != null && cnt > 0) {
            bootstrap.option(EpollChannelOption.TCP_KEEPCNT, cnt);
        }
        if (logger.isInfoEnabled()) {
            logger.info("TCP keepalive tuning enabled on {}: idle={}s, intvl={}s, cnt={}",
                    config.toSimpleString(), idle, intvl, cnt);
        }
    }

    /**
     * Resolve the idle-close threshold (milliseconds). A non-positive {@code idleTimeout}
     * configuration disables the idle-close handler entirely (legacy behaviour).
     */
    private long resolveIdleTimeoutMills() {
        Integer raw = config.getIdleTimeout();
        if (raw == null || raw <= 0) {
            return 0L;
        }
        return raw.longValue();
    }

    /**
     * Pipeline tail handler that, on an {@link IdleStateEvent}, first invalidates the
     * transport slot holding this channel so concurrent request threads see "needs
     * reconnect" immediately, then closes the channel. This shrinks the "request lands on a
     * closing channel" race window from "close completes" to "close is enqueued".
     */
    private final class IdleCloseHandler extends ChannelDuplexHandler {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                io.netty.channel.Channel ioChannel = ctx.channel();
                IdleStateEvent idleEvt = (IdleStateEvent) evt;
                // Identifying info for ops triage:
                //   * caller (this side): local socket address — uniquely identifies the
                //     consumer process even when the framework-level CallerServiceName is
                //     not propagated down to the transport layer.
                //   * callee (peer side): config.toSimpleString() (name:protocol:ip:port:network)
                //     plus the actual remote socket address, which captures DNS-resolved IP.
                logger.info("[long-link][idle-fire] state={} caller(local)={} callee={} remote={} channelId={}",
                        idleEvt.state(),
                        ioChannel.localAddress(),
                        config.toSimpleString(),
                        ioChannel.remoteAddress(),
                        ioChannel.id().asShortText());
                try {
                    com.tencent.trpc.core.transport.Channel wrapper =
                            NettyChannelManager.getOrAddChannel(ioChannel, config);
                    if (wrapper != null) {
                        invalidateChannel(wrapper);
                    }
                } catch (Throwable ex) {
                    logger.warn("[long-link][idle-fire] invalidate slot failed, caller(local)={} "
                                    + "callee={} remote={} channelId={}",
                            ioChannel.localAddress(), config.toSimpleString(),
                            ioChannel.remoteAddress(), ioChannel.id().asShortText(), ex);
                }
                // Async close on the EventLoop; log when it actually completes so ops can
                // tell apart "close enqueued" from "close finished".
                ctx.close().addListener(future -> logger.info(
                        "[long-link][idle-close] success={} caller(local)={} callee={} remote={} channelId={}"
                                + (future.isSuccess() ? "" : " cause={}"),
                        future.isSuccess(),
                        ioChannel.localAddress(), config.toSimpleString(),
                        ioChannel.remoteAddress(), ioChannel.id().asShortText(),
                        future.isSuccess() ? null : future.cause()));
                return;
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}
