package com.tencent.trpc.proto.http.constant;

import java.nio.charset.StandardCharsets;

public class Constant {

    public static final String TEST_MESSAGE = "tRPC-Java!";
    public static final String TEST_RSP_MESSAGE = "Hello tRPC-Java!";

    public static final String TEST_STRING_REQ_KEY = "stringReqKey";
    public static final String TEST_STRING_REQ_VALUE = "stringReqValue";

    public static final String TEST_BYTES_REQ_KEY = "bytesReqKey";
    public static final byte[] TEST_BYTES_REQ_VALUE = "bytesReqValue".getBytes(StandardCharsets.UTF_8);

    public static final String TEST_STRING_RSP_KEY = "stringRspKey";
    public static final String TEST_STRING_RSP_VALUE = "stringRspValue";

    public static final String TEST_BYTES_RSP_KEY = "bytesRspKey";
    public static final byte[] TEST_BYTES_RSP_VALUE = "bytesRspValue".getBytes(StandardCharsets.UTF_8);
}
