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

package com.tencent.trpc.transport.http.support.jetty;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.transport.http.AbstractHttpServer;
import com.tencent.trpc.transport.http.ExecutorDispatcher;
import com.tencent.trpc.transport.http.HttpExecutor;
import com.tencent.trpc.transport.http.common.ServletManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * This is a Jetty server based on HTTP that receives and processes HTTP requests. Support for other protocols
 * can be inherited from this class, and only the getServerConnector method needs to be overridden to support
 * different protocols.
 */
@Extension(JettyHttpServer.NAME)
public class JettyHttpServer extends AbstractHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpServer.class);

    public static final String NAME = "jetty";
    /**
     * Http server, serve for the http request.
     */
    protected Server server;

    public JettyHttpServer(ProtocolConfig config, HttpExecutor executor) {
        super(config, executor);
    }

    /**
     * Whether has bound a port
     */
    @Override
    public boolean isBound() {
        return server != null && server.isRunning();
    }

    /**
     * Get a Server connector which can support any protocol. Diffrent protocol only need to config connector.
     *
     * @param server the Jetty {@link Server}
     * @return the Jetty's {@link ServerConnector}
     */
    @Override
    public ServerConnector getServerConnector(Server server) {
        ProtocolConfig config = getConfig();
        ServerConnector connector = new ServerConnector(server);

        connector.setHost(config.getIp());
        connector.setPort(config.getPort());
        connector.setAcceptQueueSize(config.getMaxConns());
        return connector;
    }

    /**
     * Start server
     */
    @Override
    protected void doOpen() throws Exception {
        ProtocolConfig config = getConfig();

        // 1. Get server worker pool
        QueuedThreadPool threadPool = getServerThreadPool(config);

        // 2. Build server
        server = new Server(threadPool);

        // 3. Load connector, diffrent protocol need to change a diffrent connector.
        server.addConnector(getServerConnector(server));

        // 4. Init custom routes
        initCustomRoute(config);

        // 5. Start server
        server.start();
    }

    /**
     * Initialize custom route, include the executor and servlet context for each server.
     *
     * @param config protocol config
     */
    private void initCustomRoute(ProtocolConfig config) {
        ServletContextHandler context = getServletContextHandler(server);

        int port = config.getPort();
        ServletManager.getManager().addServletContext(port, context.getServletContext());
        ExecutorDispatcher.addHttpExecutor(port, this.getExecutor());
    }

    /**
     * Get the servlet context with all loaded handlers.
     *
     * @param server the Jetty {@link Server}
     * @return the Jetty's {@link ServletContextHandler}
     */
    private ServletContextHandler getServletContextHandler(Server server) {
        ServletHandler servletHandler = new ServletHandler();
        ServletHolder servletHolder =
                servletHandler.addServletWithMapping(ExecutorDispatcher.class, "/*");
        servletHolder.setAsyncSupported(true);
        servletHolder.setInitOrder(1);
        ServletContextHandler context =
                new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.setServletHandler(servletHandler);
        return context;
    }

    /**
     * Get the server worker pool. The default number of worker threads is 4, and the default timeout is 180 seconds.
     *
     * @param config protocol config
     * @return server worker pool
     */
    private QueuedThreadPool getServerThreadPool(ProtocolConfig config) {
        int threads = config.getIoThreads();
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon(true);
        threadPool.setMaxThreads(Math.max(threads, Constants.DEFAULT_MAX_THREADS));
        threadPool.setMinThreads(Math.max(threads, Constants.DEFAULT_CORE_THREADS));
        threadPool.setIdleTimeout(Math.max(config.getIdleTimeout(), Integer.parseInt(Constants.DEFAULT_IDLE_TIMEOUT)));
        return threadPool;
    }

    /**
     * Shutdown server
     */
    @Override
    protected void doClose() {
        ExecutorDispatcher.removeHttpExecutor(getConfig().getPort());
        ServletManager.getManager().removeServletContext(getConfig().getPort());
        if (getExecutor() != null) {
            try {
                getExecutor().destroy();
            } catch (Exception e) {
                logger.error("stop server|" + bindAddress + " exception", e);
            }
        }
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("stop server|" + bindAddress + " exception", e);
            }
        }
    }
}
