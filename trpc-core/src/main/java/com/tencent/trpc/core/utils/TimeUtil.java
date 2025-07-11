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

/**
 * Time utility.
 */
public class TimeUtil {

    /**
     * Convert a time unit string to milliseconds.
     *
     * @param timeString the time like 10ms/10s/10m/10h/10d.
     * @return the time in milliseconds
     */
    public static int convertTimeUnitStringToMills(String timeString) {
        if (timeString.endsWith("d")) {
            int time = Integer.parseInt(timeString.substring(0, timeString.lastIndexOf("d")));
            return time * 24 * 60 * 60 * 1000;
        }
        if (timeString.endsWith("h")) {
            int time = Integer.parseInt(timeString.substring(0, timeString.lastIndexOf("h")));
            return time * 60 * 60 * 1000;
        }
        if (timeString.endsWith("m")) {
            int time = Integer.parseInt(timeString.substring(0, timeString.lastIndexOf("m")));
            return time * 60 * 1000;
        }
        if (timeString.endsWith("ms")) {
            return Integer.parseInt(timeString.substring(0, timeString.lastIndexOf("ms")));
        }
        if (timeString.endsWith("s")) {
            int time = Integer.parseInt(timeString.substring(0, timeString.lastIndexOf("s")));
            return time * 1000;
        }
        throw new IllegalStateException("the time string " + timeString + " format error!");
    }

    /**
     * Convert a time unit string to seconds.
     *
     * @param timeString the time like 10ms/10s/10m/10h/10d.
     * @return the time in seconds
     */
    public static int convertTimeUnitStringToSeconds(String timeString) {
        int time = convertTimeUnitStringToMills(timeString);
        return time / 1000;
    }

}
