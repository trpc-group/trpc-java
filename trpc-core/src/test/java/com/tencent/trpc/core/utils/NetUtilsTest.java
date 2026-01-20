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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
public class NetUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(NetUtilsTest.class);

    @Test
    public void testNet() {
        Assertions.assertEquals(Constants.UNKNOWN, NetUtils.toIpPort(null));
        InetSocketAddress inetSocketAddress = new InetSocketAddress("9.9.8.12", 8080);
        String ipPort = NetUtils.toIpPort(inetSocketAddress);
        Assertions.assertEquals("9.9.8.12:8080", ipPort);
        int port1 = NetUtils.getAvailablePort();
        int port2 = NetUtils.getAvailablePort("127.0.0.1", 8080);
        int port3 = NetUtils.getRandomPort();
        Assertions.assertTrue(port1 < 65536);
        Assertions.assertTrue(port2 < 65536);
        Assertions.assertTrue(port3 < 65536);
    }

    @Test
    public void testGetHostIp() {
        Assertions.assertNotNull(NetUtils.getHostIp());
    }

    @Test
    public void testGetHostIpNull() throws UnknownHostException {
        try (MockedStatic<InetAddress> mockedStatic = Mockito.mockStatic(InetAddress.class)) {
            mockedStatic.when(InetAddress::getLocalHost).thenThrow(new IllegalStateException());
            Assertions.assertNull(NetUtils.getHostIp());
        }
    }

    @Test
    public void testGetHostIpV4() throws UnknownHostException {
        try (MockedStatic<InetAddress> mockedStatic = Mockito.mockStatic(InetAddress.class)) {
            InetAddress inetAddress = Mockito.mock(InetAddress.class);
            Mockito.when(inetAddress.isLoopbackAddress()).thenReturn(true);
            mockedStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);
            Assertions.assertNotNull(NetUtils.getHostIp());
        }
    }

    @Test
    public void testGetInet4Address() {
        try {
            NetUtils.getInet4Address();
        } catch (SocketException e) {
            Assertions.fail();
        }
    }

    @Test
    public void testGetInet4AddressNull() throws SocketException {
        try (MockedStatic<NetworkInterface> mockedStatic = Mockito.mockStatic(NetworkInterface.class)) {
            mockedStatic.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.emptyEnumeration());
            Assertions.assertNull(NetUtils.getInet4Address());
        }
    }

    @Test
    public void testResolveMultiNicAddr() {
        String host = NetUtils.resolveMultiNicAddr("eth0, lo0, eth1");
        logger.debug(host);
        NetUtils.resolveMultiNicAddr("lo0");
        Assertions.assertNull(NetUtils.resolveMultiNicAddr(""));
        String none = null;
        Assertions.assertNull(NetUtils.resolveMultiNicAddr(none));
    }
}
