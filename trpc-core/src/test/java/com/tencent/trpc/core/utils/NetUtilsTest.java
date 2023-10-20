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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NetUtils.class, InetAddress.class, NetworkInterface.class})
public class NetUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(NetUtilsTest.class);

    @Test
    public void testNet() {
        Assert.assertEquals(Constants.UNKNOWN, NetUtils.toIpPort(null));
        InetSocketAddress inetSocketAddress = new InetSocketAddress("9.9.8.12", 8080);
        String ipPort = NetUtils.toIpPort(inetSocketAddress);
        Assert.assertEquals("9.9.8.12:8080", ipPort);
        int port1 = NetUtils.getAvailablePort();
        int port2 = NetUtils.getAvailablePort("127.0.0.1", 8080);
        int port3 = NetUtils.getRandomPort();
        Assert.assertTrue(port1 < 65536);
        Assert.assertTrue(port2 < 65536);
        Assert.assertTrue(port3 < 65536);
    }

    @Test
    public void testGetHostIp() {
        Assert.assertNotNull(NetUtils.getHostIp());
    }

    @Test
    public void testGetHostIpNull() throws UnknownHostException {
        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getLocalHost()).thenThrow(new IllegalStateException());
        Assert.assertNull(NetUtils.getHostIp());
    }

    @Test
    public void testGetHostIpV4() throws UnknownHostException {
        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = PowerMockito.mock(InetAddress.class);
        PowerMockito.when(inetAddress.isLoopbackAddress()).thenReturn(true);
        PowerMockito.when(InetAddress.getLocalHost()).thenReturn(inetAddress);
        Assert.assertNotNull(NetUtils.getHostIp());
    }

    @Test
    public void testGetInet4Address() {
        try {
            NetUtils.getInet4Address();
        } catch (SocketException e) {
            Assert.fail();
        }
    }

    @Test
    public void testGetInet4AddressNull() throws SocketException {
        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenReturn(Collections.emptyEnumeration());
        Assert.assertNull(NetUtils.getInet4Address());
    }

    @Test
    public void testResolveMultiNicAddr() {
        String host = NetUtils.resolveMultiNicAddr("eth0, lo0, eth1");
        logger.debug(host);
        NetUtils.resolveMultiNicAddr("lo0");
        Assert.assertNull(NetUtils.resolveMultiNicAddr(""));
        String none = null;
        Assert.assertNull(NetUtils.resolveMultiNicAddr(none));
    }
}
