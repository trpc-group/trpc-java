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

import org.junit.Assert;
import org.junit.Test;

public class IPUtilsTest {

    @Test
    public void testTransfer() {
        String host = "19.12.53.243";
        int ipInt = IPUtils.ip2int(host);
        byte[] ipBytes = IPUtils.ip2bytes(host);
        String newIp = IPUtils.bytes2ip(ipBytes);
        Assert.assertEquals(newIp, host);
        String newIp2 = IPUtils.int2ip(ipInt);
        Assert.assertEquals(newIp2, host);
        NetUtils.resolveNicAddr("eth1");
        Assert.assertEquals(0, IPUtils.ip2int(null));
    }

    @Test
    public void testIsIPStrValid() {
        Assert.assertFalse(IPUtils.isIPStrValid(null));
        Assert.assertFalse(IPUtils.isIPStrValid(""));
        Assert.assertFalse(IPUtils.isIPStrValid("0.0.0.0"));
        Assert.assertFalse(IPUtils.isIPStrValid("::"));
        Assert.assertFalse(IPUtils.isIPStrValid("127.0.0.1"));
        Assert.assertFalse(IPUtils.isIPStrValid("::1"));
        Assert.assertFalse(IPUtils.isIPStrValid("169.254.1.1"));
        Assert.assertFalse(IPUtils.isIPStrValid("fe80::42:c0ff:fea8:a02"));
        Assert.assertFalse(IPUtils.isIPStrValid("172.16.0.1"));
        Assert.assertFalse(IPUtils.isIPStrValid("192.168.10.6"));
        Assert.assertFalse(IPUtils.isIPStrValid("FEC0::1:2:3:4"));
        Assert.assertFalse(IPUtils.isIPStrValid("fd12:3456:789a:1::1"));
        Assert.assertTrue(IPUtils.isIPStrValid("1.2.3.4"));
        Assert.assertTrue(IPUtils.isIPStrValid("2402:4e00::"));
    }
}
