package com.tencent.trpc.proto.http.constant;

import java.nio.charset.StandardCharsets;

public class Constant {

    public static final String TEST_MESSAGE = "tRPC-Java!";
    public static final String TEST_RSP_MESSAGE = "Hello tRPC-Java!";

    public static final String TEST_STRING_REQ_KEY = "string-req-key";
    public static final String TEST_STRING_REQ_VALUE = "stringReqValue";

    public static final String TEST_BYTES_REQ_KEY = "bytes-req-key";
    public static final byte[] TEST_BYTES_REQ_VALUE = "bytesReqValue".getBytes(StandardCharsets.UTF_8);

    public static final String TEST_STRING_RSP_KEY = "string-rsp-key";
    public static final String TEST_STRING_RSP_VALUE = "stringRspValue";

    public static final String TEST_BYTES_RSP_KEY = "bytes-rsp-key";
    public static final byte[] TEST_BYTES_RSP_VALUE = "bytesRspValue".getBytes(StandardCharsets.UTF_8);
}
