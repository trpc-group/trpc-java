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

package com.tencent.trpc.core.utils;

import com.google.common.net.InetAddresses;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * IP utility.
 */
public class IPUtils {

    public static byte[] ip2bytes(String host) {
        byte[] bytes = new byte[4];
        if (host != null && host.split("\\.").length == 4) {
            String[] ips = host.split("\\.");
            bytes[0] = (byte) (Short.parseShort(ips[0]) & 0xFF);
            bytes[1] = (byte) (Short.parseShort(ips[1]) & 0xFF);
            bytes[2] = (byte) (Short.parseShort(ips[2]) & 0xFF);
            bytes[3] = (byte) (Short.parseShort(ips[3]) & 0xFF);
        }
        return bytes;
    }

    public static String bytes2ip(byte[] b) {
        short b1 = (short) (b[0] & 0xFF);
        short b2 = (short) (b[1] & 0xFF);
        short b3 = (short) (b[2] & 0xFF);
        short b4 = (short) (b[3] & 0xFF);
        return b1 + "." + b2 + "."
                + b3 + "." + b4;
    }

    public static String int2ip(int ip) {
        short b1 = (short) (ip & 0xFF);
        short b2 = (short) ((ip & 0xFF00) >>> 8);
        short b3 = (short) ((ip & 0xFF0000) >>> 16);
        short b4 = (short) ((ip & 0xFF000000) >>> 24);
        return b4 + "." + b3 + "."
                + b2 + "." + b1;
    }

    public static int ip2int(String ip) {
        if (ip != null && ip.split("\\.").length == 4) {
            String[] ips = ip.split("\\.");
            int ret = 0;
            ret |= Short.parseShort(ips[0]) << 24;
            ret |= Short.parseShort(ips[1]) << 16;
            ret |= Short.parseShort(ips[2]) << 8;
            ret |= Short.parseShort(ips[3]);
            return ret;
        }
        return 0;
    }

    /**
     * Check if the IP is a private IP.
     *
     * @param ip the IP address to check
     * @return true if it's a private IP
     */
    public static boolean isPrivateAddress(InetAddress ip) {
        byte[] ipByte = ip.getAddress();
        if (ip instanceof Inet4Address) {
            if (((ipByte[0] & 0xFF) == 172) && ((ipByte[1] & 0xF0) == 16)) {
                return true;
            }
            if (((ipByte[0] & 0xFF) == 192) && ((ipByte[1] & 0xFF) == 168)) {
                return true;
            }
        } else if (ip instanceof Inet6Address) {
            if ((ipByte[0] & 0xFE) == 0xFC) {
                return true;
            }
            if (((ipByte[0] & 0xFF) == 0xFE) && ((ipByte[1] & 0xC0) == 0xC0)) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Check if the IP is a valid IP.
     *
     * @param ip the IP address to check
     * @return true if it's a valid IP
     */
    public static boolean isIPValid(InetAddress ip) {
        if (ip.isAnyLocalAddress()) {
            return false;
        }
        if (ip.isLoopbackAddress()) {
            return false;
        }
        if (ip.isLinkLocalAddress()) {
            return false;
        }
        if (isPrivateAddress(ip)) {
            return false;
        }
        return true;
    }

    /**
     * Check if the IP string is a valid IP.
     *
     * @param ipStr the IP string to check
     * @return true if it's a valid IP
     */
    public static boolean isIPStrValid(String ipStr) {
        if (StringUtils.isEmpty(ipStr) || !InetAddresses.isInetAddress(ipStr)) {
            // input is not an IP string
            return false;
        }
        InetAddress ip;
        try {
            // first check if it's an IP string to avoid triggering DNS query
            ip = InetAddress.getByName(ipStr);
        } catch (Exception ex) {
            return false;
        }
        return isIPValid(ip);
    }

    /**
     * Try to get the IP from the configuration.
     *
     * @return the IP from the configuration if found, null otherwise
     */
    public static String tryGetIPFromConfig() {
        ServerConfig cfg = ConfigManager.getInstance().getServerConfig();
        // try to get the IP configured under the server
        String ip = cfg.getLocalIp();
        if (IPUtils.isIPStrValid(ip)) {
            return ip;
        }
        // try to get the IP configured under each service
        for (ServiceConfig service : cfg.getServiceMap().values()) {
            ip = service.getIp();
            if (IPUtils.isIPStrValid(ip)) {
                return ip;
            }
        }
        // try to get the IP based on the NIC configured under the server
        String nic = cfg.getNic();
        ip = NetUtils.resolveNicAddr(nic);
        if (IPUtils.isIPStrValid(ip)) {
            return ip;
        }

        // try to get the IP based on the NIC configured under each service
        for (ServiceConfig service : cfg.getServiceMap().values()) {
            nic = service.getNic();
            ip = NetUtils.resolveNicAddr(nic);
            if (IPUtils.isIPStrValid(ip)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * Try to get the IP from the network interface.
     *
     * @return the IP from the network interface if found, null otherwise
     */
    public static String tryGetIPFromInterface() {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        String ipv4 = null;
        String ipv6 = null;
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress ip = addresses.nextElement();
                if (IPUtils.isIPValid(ip)) {
                    if (ip instanceof Inet4Address) {
                        ipv4 = ip.getHostAddress();
                    } else if (ip instanceof Inet6Address) {
                        ipv6 = ip.getHostAddress();
                    }
                }
            }
        }

        // prioritize returning ipv4
        if (ipv4 != null) {
            return ipv4;
        } else if (ipv6 != null) {
            return ipv6;
        }
        return null;
    }

}
