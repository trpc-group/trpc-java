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
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Net utility.
 */
public class NetUtils {

    public static final String LOCAL_HOST = "127.0.0.1";
    public static final String ANY_HOST = "0.0.0.0";
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);
    /**
     * Random port start and end.
     */
    private static final int RND_PORT_START = 10000;
    private static final int RND_PORT_END = 60000;
    /**
     * Minimum port and maximum port.
     */
    private static final int MAX_PORT = 65535;

    /**
     * Convert InetSocketAddress to IP:Port string.
     *
     * @param address the InetSocketAddress to convert
     * @return the IP:Port string
     */
    public static String toIpPort(InetSocketAddress address) {
        if (address == null) {
            return Constants.UNKNOWN;
        }
        return address.getAddress().getHostAddress() + Constants.COLON + address.getPort();
    }

    /**
     * Get an available port.
     *
     * @return an available port
     */
    public static int getAvailablePort() {
        try (ServerSocket ss = new ServerSocket()) {
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        }
    }

    /**
     * Get an available port in the system.
     * The minimum port value is 10000, and the maximum port value is 65535.
     *
     * @param startPort the starting port
     * @return an available port
     */
    public static int getAvailablePort(int startPort) {
        return getAvailablePort("localhost", startPort);
    }

    public static int getAvailablePort(String host, int port) {
        if (port <= 0) {
            return getAvailablePort();
        }
        for (int i = port; i < MAX_PORT; i++) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.bind(new InetSocketAddress(host, i));
                return i;
            } catch (IOException e) {
                logger.error("note: " + port + " has been used, while find next");
            }
        }
        return port;
    }

    public static int getRandomPort() {
        return RND_PORT_START + ThreadLocalRandom.current().nextInt(RND_PORT_END - RND_PORT_START);
    }

    /**
     * Resolve multiple NICs. Returns the first available NIC's IP.
     *
     * <p>Example: If NICs is "eth0, eth1, eth2" and eth1 is available, it will return eth1's IP.</p>
     *
     * @param nics multiple NIC string
     * @return first available NIC's IP
     */
    public static String resolveMultiNicAddr(String nics) {
        if (nics == null) {
            return null;
        }
        return resolveMultiNicAddr(StringUtils.splitToWords(nics));
    }

    /**
     * Resolve multiple NICs. Returns the first available NIC's IP.
     *
     * <p>Example: If NICs is "eth0, eth1, eth2" and eth1 is available, it will return eth1's IP</p>
     *
     * @param nics multiple NIC string
     * @return first available NIC's IP
     */
    public static String resolveMultiNicAddr(String[] nics) {
        for (String nic : nics) {
            String ip = resolveNicAddr(nic);
            if (ip != null) {
                logger.debug("nics: {}, resolve nic : {}, ip: {}", nics, nic, ip);
                return ip;
            }
        }
        return null;
    }

    /**
     * Resolve NIC IP.
     *
     * @param nic NIC identifier, such as eth1
     */
    public static String resolveNicAddr(String nic) {
        try {
            NetworkInterface ni = NetworkInterface.getByName(nic);
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress i = addrs.nextElement();
                if (i instanceof Inet4Address) {
                    return i.getHostAddress();
                }
            }
            addrs = ni.getInetAddresses();
            return addrs.nextElement().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get host address.
     *
     * @return the host IP address, or null if not found
     */
    public static String getHostIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            // if it is a loopback address, get the IPv4 address
            if (address.isLoopbackAddress()) {
                address = getInet4Address();
            }
            if (address != null) {
                logger.info("get host ip success, address:{}", address);
                return address.getHostAddress();
            }
        } catch (Exception e) {
            logger.error("get host ip failure:", e);
        }

        return null;
    }

    /**
     * Get IPv4 network configuration.
     *
     * @return an InetAddress object representing the IPv4 address, or null if not found
     * @throws SocketException if there is a problem retrieving the network interfaces
     */
    public static InetAddress getInet4Address() throws SocketException {
        // Get information about all network interfaces
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress ip = addresses.nextElement();
                if (ip instanceof Inet4Address) {
                    return ip;
                }
            }
        }
        return null;
    }

}
