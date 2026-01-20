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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IPUtilsTest {

    @Test
    public void testTransfer() {
        String host = "19.12.53.243";
        int ipInt = IPUtils.ip2int(host);
        byte[] ipBytes = IPUtils.ip2bytes(host);
        String newIp = IPUtils.bytes2ip(ipBytes);
        Assertions.assertEquals(newIp, host);
        String newIp2 = IPUtils.int2ip(ipInt);
        Assertions.assertEquals(newIp2, host);
        NetUtils.resolveNicAddr("eth1");
        Assertions.assertEquals(0, IPUtils.ip2int(null));
    }

    @Test
    public void testIsIPStrValid() {
        Assertions.assertFalse(IPUtils.isIPStrValid(null));
        Assertions.assertFalse(IPUtils.isIPStrValid(""));
        Assertions.assertFalse(IPUtils.isIPStrValid("0.0.0.0"));
        Assertions.assertFalse(IPUtils.isIPStrValid("::"));
        Assertions.assertFalse(IPUtils.isIPStrValid("127.0.0.1"));
        Assertions.assertFalse(IPUtils.isIPStrValid("::1"));
        Assertions.assertFalse(IPUtils.isIPStrValid("169.254.1.1"));
        Assertions.assertFalse(IPUtils.isIPStrValid("fe80::42:c0ff:fea8:a02"));
        Assertions.assertFalse(IPUtils.isIPStrValid("172.16.0.1"));
        Assertions.assertFalse(IPUtils.isIPStrValid("192.168.10.6"));
        Assertions.assertFalse(IPUtils.isIPStrValid("FEC0::1:2:3:4"));
        Assertions.assertFalse(IPUtils.isIPStrValid("fd12:3456:789a:1::1"));
        Assertions.assertTrue(IPUtils.isIPStrValid("1.2.3.4"));
        Assertions.assertTrue(IPUtils.isIPStrValid("2402:4e00::"));
    }
}
