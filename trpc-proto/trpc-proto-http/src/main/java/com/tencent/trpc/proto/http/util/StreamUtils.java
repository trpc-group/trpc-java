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

package com.tencent.trpc.proto.http.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Simple utility methods for dealing with streams.
 */
public class StreamUtils {

    /**
     * The default buffer size used when copying bytes.
     */
    public static final int BUFFER_SIZE = 4096;

    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     *
     * @param in the InputStream to copy from
     * @param out the OutputStream to copy to
     * @return the number of bytes copied
     * @throws IOException in case of I/O errors
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(in, "No InputStream specified");
        Objects.requireNonNull(out, "No OutputStream specified");

        int byteCount = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

    /**
     * Copy the contents of the given InputStream into a new byte array.
     *
     * @param in the InputStream to copy from
     * @param contentLen the expected content length
     * @return the new byte array that has been copied to
     * @throws IOException in case of I/O errors
     */
    public static byte[] readAllBytes(InputStream in, int contentLen) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(contentLen > 0 ? contentLen : StreamUtils.BUFFER_SIZE);
        copy(in, bos);
        return bos.toByteArray();
    }

}
