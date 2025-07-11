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

import org.apache.commons.text.StringTokenizer;

/**
 * String utility.
 */
public class StringUtils {

    public static final String EMPTY = "";

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isJavaIdentifier(String s) {
        if (isEmpty(s) || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Split string to words. It will be strip input.
     *
     * <p>If input is Comma Separated Value, it returns words which is split with comma.
     * Or if input is common string, it returns one word.
     * Both of them will strip input.</p>
     *
     * <p>Example:</p>
     * <pre>
     *     1. "a,b,c" split to ["a", "b", "c"]
     *     2. " a, b , c " split to ["a", "b", "c"]
     *     3. "a" split to ["a"]
     *     4. "  a    " split to ["a"]
     *     5. null split to []
     * </pre>
     *
     * @param input must be Comma Separated Value.
     * @return words after split
     */
    public static String[] splitToWords(String input) {
        return StringTokenizer.getCSVInstance(input).getTokenArray();
    }

}
