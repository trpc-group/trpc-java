package com.tencent.trpc.proto.standard.common;

import com.google.protobuf.ByteString;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TRPCProtocolTest {
    @Test
    public void testTrpcMagicEnum() {
        assertEquals(0, TRPCProtocol.TrpcMagic.TRPC_DEFAULT_NONE.getNumber());
        assertEquals(2352, TRPCProtocol.TrpcMagic.TRPC_MAGIC_VALUE.getNumber());

        assertEquals(TRPCProtocol.TrpcMagic.TRPC_DEFAULT_NONE, TRPCProtocol.TrpcMagic.forNumber(0));
        assertEquals(TRPCProtocol.TrpcMagic.TRPC_MAGIC_VALUE, TRPCProtocol.TrpcMagic.forNumber(2352));
        assertNull(TRPCProtocol.TrpcMagic.forNumber(-1));
    }

    @Test
    public void testTrpcDataFrameTypeEnum() {
        assertEquals(0, TRPCProtocol.TrpcDataFrameType.TRPC_UNARY_FRAME.getNumber());
        assertEquals(1, TRPCProtocol.TrpcDataFrameType.TRPC_STREAM_FRAME.getNumber());

        assertEquals(TRPCProtocol.TrpcDataFrameType.TRPC_UNARY_FRAME, TRPCProtocol.TrpcDataFrameType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcDataFrameType.TRPC_STREAM_FRAME, TRPCProtocol.TrpcDataFrameType.forNumber(1));
        assertNull(TRPCProtocol.TrpcDataFrameType.forNumber(-1));
    }

    @Test
    public void testTrpcStreamFrameTypeEnum() {
        assertEquals(0, TRPCProtocol.TrpcStreamFrameType.TRPC_UNARY.getNumber());
        assertEquals(1, TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT.getNumber());
        assertEquals(2, TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_DATA.getNumber());
        assertEquals(3, TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_FEEDBACK.getNumber());
        assertEquals(4, TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_CLOSE.getNumber());

        assertEquals(TRPCProtocol.TrpcStreamFrameType.TRPC_UNARY, TRPCProtocol.TrpcStreamFrameType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT, TRPCProtocol.TrpcStreamFrameType.forNumber(1));
        assertEquals(TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_DATA, TRPCProtocol.TrpcStreamFrameType.forNumber(2));
        assertEquals(TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_FEEDBACK, TRPCProtocol.TrpcStreamFrameType.forNumber(3));
        assertEquals(TRPCProtocol.TrpcStreamFrameType.TRPC_STREAM_FRAME_CLOSE, TRPCProtocol.TrpcStreamFrameType.forNumber(4));
        assertNull(TRPCProtocol.TrpcStreamFrameType.forNumber(-1));
    }

    @Test
    public void testTrpcStreamCloseTypeEnum() {
        assertEquals(0, TRPCProtocol.TrpcStreamCloseType.TRPC_STREAM_CLOSE.getNumber());
        assertEquals(1, TRPCProtocol.TrpcStreamCloseType.TRPC_STREAM_RESET.getNumber());

        assertEquals(TRPCProtocol.TrpcStreamCloseType.TRPC_STREAM_CLOSE, TRPCProtocol.TrpcStreamCloseType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcStreamCloseType.TRPC_STREAM_RESET, TRPCProtocol.TrpcStreamCloseType.forNumber(1));
        assertNull(TRPCProtocol.TrpcStreamCloseType.forNumber(-1));
    }

    @Test
    public void testTrpcProtoVersionEnum() {
        assertEquals(0, TRPCProtocol.TrpcProtoVersion.TRPC_PROTO_V1.getNumber());

        assertEquals(TRPCProtocol.TrpcProtoVersion.TRPC_PROTO_V1, TRPCProtocol.TrpcProtoVersion.forNumber(0));
        assertNull(TRPCProtocol.TrpcProtoVersion.forNumber(-1));
    }

    @Test
    public void testTrpcCallTypeEnum() {
        assertEquals(0, TRPCProtocol.TrpcCallType.TRPC_UNARY_CALL.getNumber());
        assertEquals(1, TRPCProtocol.TrpcCallType.TRPC_ONEWAY_CALL.getNumber());

        assertEquals(TRPCProtocol.TrpcCallType.TRPC_UNARY_CALL, TRPCProtocol.TrpcCallType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcCallType.TRPC_ONEWAY_CALL, TRPCProtocol.TrpcCallType.forNumber(1));
        assertNull(TRPCProtocol.TrpcCallType.forNumber(-1));
    }

    @Test
    public void testTrpcMessageTypeEnum() {
        assertEquals(0, TRPCProtocol.TrpcMessageType.TRPC_DEFAULT.getNumber());
        assertEquals(1, TRPCProtocol.TrpcMessageType.TRPC_DYEING_MESSAGE.getNumber());
        assertEquals(2, TRPCProtocol.TrpcMessageType.TRPC_TRACE_MESSAGE.getNumber());
        assertEquals(4, TRPCProtocol.TrpcMessageType.TRPC_MULTI_ENV_MESSAGE.getNumber());
        assertEquals(8, TRPCProtocol.TrpcMessageType.TRPC_GRID_MESSAGE.getNumber());
        assertEquals(16, TRPCProtocol.TrpcMessageType.TRPC_SETNAME_MESSAGE.getNumber());

        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_DEFAULT, TRPCProtocol.TrpcMessageType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_DYEING_MESSAGE, TRPCProtocol.TrpcMessageType.forNumber(1));
        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_TRACE_MESSAGE, TRPCProtocol.TrpcMessageType.forNumber(2));
        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_MULTI_ENV_MESSAGE, TRPCProtocol.TrpcMessageType.forNumber(4));
        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_GRID_MESSAGE, TRPCProtocol.TrpcMessageType.forNumber(8));
        assertEquals(TRPCProtocol.TrpcMessageType.TRPC_SETNAME_MESSAGE, TRPCProtocol.TrpcMessageType.forNumber(16));
        assertNull(TRPCProtocol.TrpcMessageType.forNumber(-1));
    }

    @Test
    public void testTrpcContentEncodeType() {
        // Test getNumber method
        assertEquals(0, TRPCProtocol.TrpcContentEncodeType.TRPC_PROTO_ENCODE.getNumber());
        assertEquals(1, TRPCProtocol.TrpcContentEncodeType.TRPC_JCE_ENCODE.getNumber());
        assertEquals(2, TRPCProtocol.TrpcContentEncodeType.TRPC_JSON_ENCODE.getNumber());
        assertEquals(3, TRPCProtocol.TrpcContentEncodeType.TRPC_FLATBUFFER_ENCODE.getNumber());
        assertEquals(4, TRPCProtocol.TrpcContentEncodeType.TRPC_NOOP_ENCODE.getNumber());
        assertEquals(5, TRPCProtocol.TrpcContentEncodeType.TRPC_XML_ENCODE.getNumber());
        assertEquals(6, TRPCProtocol.TrpcContentEncodeType.TRPC_THRIFT_ENCODE.getNumber());

        // Test forNumber method
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_PROTO_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_JCE_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(1));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_JSON_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(2));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_FLATBUFFER_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(3));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_NOOP_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(4));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_XML_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(5));
        assertEquals(TRPCProtocol.TrpcContentEncodeType.TRPC_THRIFT_ENCODE, TRPCProtocol.TrpcContentEncodeType.forNumber(6));
        assertNull(TRPCProtocol.TrpcContentEncodeType.forNumber(7));
    }

    @Test
    public void testTrpcCompressType() {
        // Test getNumber method
        assertEquals(0, TRPCProtocol.TrpcCompressType.TRPC_DEFAULT_COMPRESS.getNumber());
        assertEquals(1, TRPCProtocol.TrpcCompressType.TRPC_GZIP_COMPRESS.getNumber());
        assertEquals(2, TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_COMPRESS.getNumber());
        assertEquals(3, TRPCProtocol.TrpcCompressType.TRPC_ZLIB_COMPRESS.getNumber());
        assertEquals(4, TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_STREAM_COMPRESS.getNumber());
        assertEquals(5, TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_BLOCK_COMPRESS.getNumber());
        assertEquals(6, TRPCProtocol.TrpcCompressType.TRPC_LZ4_FRAME_COMPRESS.getNumber());
        assertEquals(7, TRPCProtocol.TrpcCompressType.TRPC_LZ4_BLOCK_COMPRESS.getNumber());

        // Test forNumber method
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_DEFAULT_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(0));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_GZIP_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(1));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(2));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_ZLIB_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(3));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_STREAM_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(4));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_SNAPPY_BLOCK_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(5));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_LZ4_FRAME_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(6));
        assertEquals(TRPCProtocol.TrpcCompressType.TRPC_LZ4_BLOCK_COMPRESS, TRPCProtocol.TrpcCompressType.forNumber(7));
        assertNull(TRPCProtocol.TrpcCompressType.forNumber(8));
    }

    @Test
    public void testTrpcRetCode() {
        // Test getNumber method
        assertEquals(0, TRPCProtocol.TrpcRetCode.TRPC_INVOKE_SUCCESS.getNumber());
        assertEquals(1, TRPCProtocol.TrpcRetCode.TRPC_SERVER_DECODE_ERR.getNumber());
        assertEquals(2, TRPCProtocol.TrpcRetCode.TRPC_SERVER_ENCODE_ERR.getNumber());
        assertEquals(11, TRPCProtocol.TrpcRetCode.TRPC_SERVER_NOSERVICE_ERR.getNumber());
        assertEquals(12, TRPCProtocol.TrpcRetCode.TRPC_SERVER_NOFUNC_ERR.getNumber());
        assertEquals(21, TRPCProtocol.TrpcRetCode.TRPC_SERVER_TIMEOUT_ERR.getNumber());
        assertEquals(22, TRPCProtocol.TrpcRetCode.TRPC_SERVER_OVERLOAD_ERR.getNumber());
        assertEquals(23, TRPCProtocol.TrpcRetCode.TRPC_SERVER_LIMITED_ERR.getNumber());
        assertEquals(24, TRPCProtocol.TrpcRetCode.TRPC_SERVER_FULL_LINK_TIMEOUT_ERR.getNumber());
        assertEquals(31, TRPCProtocol.TrpcRetCode.TRPC_SERVER_SYSTEM_ERR.getNumber());
        assertEquals(41, TRPCProtocol.TrpcRetCode.TRPC_SERVER_AUTH_ERR.getNumber());
        assertEquals(51, TRPCProtocol.TrpcRetCode.TRPC_SERVER_VALIDATE_ERR.getNumber());
        assertEquals(101, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR.getNumber());
        assertEquals(102, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_FULL_LINK_TIMEOUT_ERR.getNumber());
        assertEquals(111, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_CONNECT_ERR.getNumber());
        assertEquals(121, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_ENCODE_ERR.getNumber());
        assertEquals(122, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_DECODE_ERR.getNumber());
        assertEquals(123, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_LIMITED_ERR.getNumber());
        assertEquals(124, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_OVERLOAD_ERR.getNumber());
        assertEquals(131, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_ROUTER_ERR.getNumber());
        assertEquals(141, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_NETWORK_ERR.getNumber());
        assertEquals(151, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_VALIDATE_ERR.getNumber());
        assertEquals(161, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_CANCELED_ERR.getNumber());
        assertEquals(171, TRPCProtocol.TrpcRetCode.TRPC_CLIENT_READ_FRAME_ERR.getNumber());
        assertEquals(201, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_NETWORK_ERR.getNumber());
        assertEquals(211, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_MSG_EXCEED_LIMIT_ERR.getNumber());
        assertEquals(221, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_ENCODE_ERR.getNumber());
        assertEquals(222, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_DECODE_ERR.getNumber());
        assertEquals(231, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_END.getNumber());
        assertEquals(232, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_OVERFLOW_ERR.getNumber());
        assertEquals(233, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_CLOSE_ERR.getNumber());
        assertEquals(234, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_TIMEOUT_ERR.getNumber());
        assertEquals(251, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_END.getNumber());
        assertEquals(252, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_CLOSE_ERR.getNumber());
        assertEquals(253, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_EMPTY_ERR.getNumber());
        assertEquals(254, TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_TIMEOUT_ERR.getNumber());
        assertEquals(301, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_NETWORK_ERR.getNumber());
        assertEquals(311, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_MSG_EXCEED_LIMIT_ERR.getNumber());
        assertEquals(321, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_ENCODE_ERR.getNumber());
        assertEquals(322, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_DECODE_ERR.getNumber());
        assertEquals(331, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_END.getNumber());
        assertEquals(332, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_OVERFLOW_ERR.getNumber());
        assertEquals(333, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_CLOSE_ERR.getNumber());
        assertEquals(334, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_TIMEOUT_ERR.getNumber());
        assertEquals(351, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_END.getNumber());
        assertEquals(352, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_CLOSE_ERR.getNumber());
        assertEquals(353, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_EMPTY_ERR.getNumber());
        assertEquals(354, TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_TIMEOUT_ERR.getNumber());
        assertEquals(999, TRPCProtocol.TrpcRetCode.TRPC_INVOKE_UNKNOWN_ERR.getNumber());
        assertEquals(1000, TRPCProtocol.TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR.getNumber());

        // Test forNumber method
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_INVOKE_SUCCESS, TRPCProtocol.TrpcRetCode.forNumber(0));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_DECODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(1));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_ENCODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(2));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_NOSERVICE_ERR, TRPCProtocol.TrpcRetCode.forNumber(11));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_NOFUNC_ERR, TRPCProtocol.TrpcRetCode.forNumber(12));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(21));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_OVERLOAD_ERR, TRPCProtocol.TrpcRetCode.forNumber(22));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_LIMITED_ERR, TRPCProtocol.TrpcRetCode.forNumber(23));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_FULL_LINK_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(24));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_SYSTEM_ERR, TRPCProtocol.TrpcRetCode.forNumber(31));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_AUTH_ERR, TRPCProtocol.TrpcRetCode.forNumber(41));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_SERVER_VALIDATE_ERR, TRPCProtocol.TrpcRetCode.forNumber(51));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(101));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_FULL_LINK_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(102));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_CONNECT_ERR, TRPCProtocol.TrpcRetCode.forNumber(111));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_ENCODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(121));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_DECODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(122));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_LIMITED_ERR, TRPCProtocol.TrpcRetCode.forNumber(123));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_OVERLOAD_ERR, TRPCProtocol.TrpcRetCode.forNumber(124));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_ROUTER_ERR, TRPCProtocol.TrpcRetCode.forNumber(131));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_NETWORK_ERR, TRPCProtocol.TrpcRetCode.forNumber(141));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_VALIDATE_ERR, TRPCProtocol.TrpcRetCode.forNumber(151));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_CANCELED_ERR, TRPCProtocol.TrpcRetCode.forNumber(161));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_CLIENT_READ_FRAME_ERR, TRPCProtocol.TrpcRetCode.forNumber(171));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_NETWORK_ERR, TRPCProtocol.TrpcRetCode.forNumber(201));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_MSG_EXCEED_LIMIT_ERR, TRPCProtocol.TrpcRetCode.forNumber(211));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_ENCODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(221));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_DECODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(222));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_END, TRPCProtocol.TrpcRetCode.forNumber(231));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_OVERFLOW_ERR, TRPCProtocol.TrpcRetCode.forNumber(232));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_CLOSE_ERR, TRPCProtocol.TrpcRetCode.forNumber(233));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_WRITE_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(234));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_END, TRPCProtocol.TrpcRetCode.forNumber(251));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_CLOSE_ERR, TRPCProtocol.TrpcRetCode.forNumber(252));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_EMPTY_ERR, TRPCProtocol.TrpcRetCode.forNumber(253));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_SERVER_READ_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(254));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_NETWORK_ERR, TRPCProtocol.TrpcRetCode.forNumber(301));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_MSG_EXCEED_LIMIT_ERR, TRPCProtocol.TrpcRetCode.forNumber(311));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_ENCODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(321));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_DECODE_ERR, TRPCProtocol.TrpcRetCode.forNumber(322));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_END, TRPCProtocol.TrpcRetCode.forNumber(331));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_OVERFLOW_ERR, TRPCProtocol.TrpcRetCode.forNumber(332));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_CLOSE_ERR, TRPCProtocol.TrpcRetCode.forNumber(333));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_WRITE_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(334));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_END, TRPCProtocol.TrpcRetCode.forNumber(351));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_CLOSE_ERR, TRPCProtocol.TrpcRetCode.forNumber(352));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_EMPTY_ERR, TRPCProtocol.TrpcRetCode.forNumber(353));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_CLIENT_READ_TIMEOUT_ERR, TRPCProtocol.TrpcRetCode.forNumber(354));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_INVOKE_UNKNOWN_ERR, TRPCProtocol.TrpcRetCode.forNumber(999));
        assertEquals(TRPCProtocol.TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR, TRPCProtocol.TrpcRetCode.forNumber(1000));
        assertNull(TRPCProtocol.TrpcRetCode.forNumber(-1)); // Test for an invalid value
    }

    @Test
    public void testBuilder() {
        // Create a new builder instance
        TRPCProtocol.TrpcStreamInitRequestMeta.Builder builder = TRPCProtocol.TrpcStreamInitRequestMeta.newBuilder();

        // Test default values
        assertEquals(ByteString.EMPTY, builder.getCaller());
        assertEquals(ByteString.EMPTY, builder.getCallee());
        assertEquals(ByteString.EMPTY, builder.getFunc());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());

        // Set values
        ByteString caller = ByteString.copyFromUtf8("trpc.app.server.service");
        ByteString callee = ByteString.copyFromUtf8("trpc.app.server.service.interface");
        ByteString func = ByteString.copyFromUtf8("/package.service/interface");
        int messageType = 1;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");

        builder.setCaller(caller);
        builder.setCallee(callee);
        builder.setFunc(func);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);

        // Test set values
        assertEquals(caller, builder.getCaller());
        assertEquals(callee, builder.getCallee());
        assertEquals(func, builder.getFunc());
        assertEquals(messageType, builder.getMessageType());
        assertEquals(transInfoValue, builder.getTransInfoOrThrow("trpc-key"));

        // Build the message
        TRPCProtocol.TrpcStreamInitRequestMeta message = builder.build();

        // Test built message
        assertEquals(caller, message.getCaller());
        assertEquals(callee, message.getCallee());
        assertEquals(func, message.getFunc());
        assertEquals(messageType, message.getMessageType());
        assertEquals(transInfoValue, message.getTransInfoOrThrow("trpc-key"));

        // Test clear method
        builder.clear();
        assertEquals(ByteString.EMPTY, builder.getCaller());
        assertEquals(ByteString.EMPTY, builder.getCallee());
        assertEquals(ByteString.EMPTY, builder.getFunc());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());

        // Test mergeFrom method
        TRPCProtocol.TrpcStreamInitRequestMeta.Builder builder2 = TRPCProtocol.TrpcStreamInitRequestMeta.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(caller, builder2.getCaller());
        assertEquals(callee, builder2.getCallee());
        assertEquals(func, builder2.getFunc());
        assertEquals(messageType, builder2.getMessageType());
        assertEquals(transInfoValue, builder2.getTransInfoOrThrow("trpc-key"));
    }

    @Test
    public void testTransInfoMap() {
        // Create a new builder instance
        TRPCProtocol.TrpcStreamInitRequestMeta.Builder builder = TRPCProtocol.TrpcStreamInitRequestMeta.newBuilder();

        // Test putTransInfo and getTransInfo
        ByteString value1 = ByteString.copyFromUtf8("value1");
        builder.putTransInfo("key1", value1);
        assertEquals(value1, builder.getTransInfoOrThrow("key1"));

        // Test putAllTransInfo
        ByteString value2 = ByteString.copyFromUtf8("value2");
        Map<String, ByteString> transInfoMap = new HashMap<>();
        transInfoMap.put("key2", value2);
        builder.putAllTransInfo(transInfoMap);
        assertEquals(value2, builder.getTransInfoOrThrow("key2"));

        // Test removeTransInfo
        builder.removeTransInfo("key1");
        assertFalse(builder.containsTransInfo("key1"));

        // Test clearTransInfo
        builder.clearTransInfo();
        assertTrue(builder.getTransInfoMap().isEmpty());
    }

    @Test
    public void testTrpcStreamInitResponseMetaDefaultInstance() {
        // Get the default instance
        TRPCProtocol.TrpcStreamInitResponseMeta defaultInstance = TRPCProtocol.TrpcStreamInitResponseMeta.getDefaultInstance();

        // Test default values
        assertEquals(0, defaultInstance.getRet());
        assertEquals(ByteString.EMPTY, defaultInstance.getErrorMsg());
    }

    @Test
    public void testTrpcStreamInitResponseMetaBuilder() {
        // Create a new builder instance
        TRPCProtocol.TrpcStreamInitResponseMeta.Builder builder = TRPCProtocol.TrpcStreamInitResponseMeta.newBuilder();

        // Test default values
        assertEquals(0, builder.getRet());
        assertEquals(ByteString.EMPTY, builder.getErrorMsg());

        // Set values
        int ret = 1;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");

        builder.setRet(ret);
        builder.setErrorMsg(errorMsg);

        // Test set values
        assertEquals(ret, builder.getRet());
        assertEquals(errorMsg, builder.getErrorMsg());

        // Build the message
        TRPCProtocol.TrpcStreamInitResponseMeta message = builder.build();

        // Test built message
        assertEquals(ret, message.getRet());
        assertEquals(errorMsg, message.getErrorMsg());

        // Test clear method
        builder.clear();
        assertEquals(0, builder.getRet());
        assertEquals(ByteString.EMPTY, builder.getErrorMsg());

        // Test mergeFrom method
        TRPCProtocol.TrpcStreamInitResponseMeta.Builder builder2 = TRPCProtocol.TrpcStreamInitResponseMeta.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(ret, builder2.getRet());
        assertEquals(errorMsg, builder2.getErrorMsg());
    }

    @Test
    public void testTrpcStreamInitResponseMetaParsing() throws Exception {
        // Create a new builder instance and set values
        TRPCProtocol.TrpcStreamInitResponseMeta.Builder builder = TRPCProtocol.TrpcStreamInitResponseMeta.newBuilder();
        int ret = 1;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");
        builder.setRet(ret);
        builder.setErrorMsg(errorMsg);

        // Build the message
        TRPCProtocol.TrpcStreamInitResponseMeta message = builder.build();

        // Serialize the message to byte array
        byte[] data = message.toByteArray();

        // Parse the message from byte array
        TRPCProtocol.TrpcStreamInitResponseMeta parsedMessage = TRPCProtocol.TrpcStreamInitResponseMeta.parseFrom(data);

        // Test parsed message
        assertEquals(ret, parsedMessage.getRet());
        assertEquals(errorMsg, parsedMessage.getErrorMsg());
    }

    @Test
    public void testTrpcStreamInitResponseMetaEqualsAndHashCode() {
        // Create two builder instances and set values
        TRPCProtocol.TrpcStreamInitResponseMeta.Builder builder1 = TRPCProtocol.TrpcStreamInitResponseMeta.newBuilder();
        TRPCProtocol.TrpcStreamInitResponseMeta.Builder builder2 = TRPCProtocol.TrpcStreamInitResponseMeta.newBuilder();
        int ret = 1;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");
        builder1.setRet(ret);
        builder1.setErrorMsg(errorMsg);
        builder2.setRet(ret);
        builder2.setErrorMsg(errorMsg);

        // Build the messages
        TRPCProtocol.TrpcStreamInitResponseMeta message1 = builder1.build();
        TRPCProtocol.TrpcStreamInitResponseMeta message2 = builder2.build();

        // Test equals and hashCode
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    public void testTrpcStreamFeedBackMetaDefaultInstance() {
        // Get the default instance
        TRPCProtocol.TrpcStreamFeedBackMeta defaultInstance = TRPCProtocol.TrpcStreamFeedBackMeta.getDefaultInstance();

        // Test default values
        assertEquals(0, defaultInstance.getWindowSizeIncrement());
    }

    @Test
    public void testTrpcStreamFeedBackMetaBuilder() {
        // Create a new builder instance
        TRPCProtocol.TrpcStreamFeedBackMeta.Builder builder = TRPCProtocol.TrpcStreamFeedBackMeta.newBuilder();

        // Test default values
        assertEquals(0, builder.getWindowSizeIncrement());

        // Set values
        int windowSizeIncrement = 1024;

        builder.setWindowSizeIncrement(windowSizeIncrement);

        // Test set values
        assertEquals(windowSizeIncrement, builder.getWindowSizeIncrement());

        // Build the message
        TRPCProtocol.TrpcStreamFeedBackMeta message = builder.build();

        // Test built message
        assertEquals(windowSizeIncrement, message.getWindowSizeIncrement());

        // Test clear method
        builder.clear();
        assertEquals(0, builder.getWindowSizeIncrement());

        // Test mergeFrom method
        TRPCProtocol.TrpcStreamFeedBackMeta.Builder builder2 = TRPCProtocol.TrpcStreamFeedBackMeta.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(windowSizeIncrement, builder2.getWindowSizeIncrement());
    }

    @Test
    public void testTrpcStreamFeedBackMetaParsing() throws Exception {
        // Create a new builder instance and set values
        TRPCProtocol.TrpcStreamFeedBackMeta.Builder builder = TRPCProtocol.TrpcStreamFeedBackMeta.newBuilder();
        int windowSizeIncrement = 1024;
        builder.setWindowSizeIncrement(windowSizeIncrement);

        // Build the message
        TRPCProtocol.TrpcStreamFeedBackMeta message = builder.build();

        // Serialize the message to byte array
        byte[] data = message.toByteArray();

        // Parse the message from byte array
        TRPCProtocol.TrpcStreamFeedBackMeta parsedMessage = TRPCProtocol.TrpcStreamFeedBackMeta.parseFrom(data);

        // Test parsed message
        assertEquals(windowSizeIncrement, parsedMessage.getWindowSizeIncrement());
    }

    @Test
    public void testTrpcStreamFeedBackMetaEqualsAndHashCode() {
        // Create two builder instances and set values
        TRPCProtocol.TrpcStreamFeedBackMeta.Builder builder1 = TRPCProtocol.TrpcStreamFeedBackMeta.newBuilder();
        TRPCProtocol.TrpcStreamFeedBackMeta.Builder builder2 = TRPCProtocol.TrpcStreamFeedBackMeta.newBuilder();
        int windowSizeIncrement = 1024;
        builder1.setWindowSizeIncrement(windowSizeIncrement);
        builder2.setWindowSizeIncrement(windowSizeIncrement);

        // Build the messages
        TRPCProtocol.TrpcStreamFeedBackMeta message1 = builder1.build();
        TRPCProtocol.TrpcStreamFeedBackMeta message2 = builder2.build();

        // Test equals and hashCode
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    public void testTrpcStreamCloseMetaDefaultInstance() {
        // Get the default instance
        TRPCProtocol.TrpcStreamCloseMeta defaultInstance = TRPCProtocol.TrpcStreamCloseMeta.getDefaultInstance();

        // Test default values
        assertEquals(0, defaultInstance.getCloseType());
        assertEquals(0, defaultInstance.getRet());
        assertEquals(ByteString.EMPTY, defaultInstance.getMsg());
        assertEquals(0, defaultInstance.getMessageType());
        assertTrue(defaultInstance.getTransInfoMap().isEmpty());
        assertEquals(0, defaultInstance.getFuncRet());
    }

    @Test
    public void testTrpcStreamCloseMetaBuilder() {
        // Create a new builder instance
        TRPCProtocol.TrpcStreamCloseMeta.Builder builder = TRPCProtocol.TrpcStreamCloseMeta.newBuilder();

        // Test default values
        assertEquals(0, builder.getCloseType());
        assertEquals(0, builder.getRet());
        assertEquals(ByteString.EMPTY, builder.getMsg());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getFuncRet());

        // Set values
        int closeType = 1;
        int ret = 2;
        ByteString msg = ByteString.copyFromUtf8("Error message");
        int messageType = 3;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int funcRet = 4;

        builder.setCloseType(closeType);
        builder.setRet(ret);
        builder.setMsg(msg);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setFuncRet(funcRet);

        // Test set values
        assertEquals(closeType, builder.getCloseType());
        assertEquals(ret, builder.getRet());
        assertEquals(msg, builder.getMsg());
        assertEquals(messageType, builder.getMessageType());
        assertEquals(transInfoValue, builder.getTransInfoOrThrow("trpc-key"));
        assertEquals(funcRet, builder.getFuncRet());

        // Build the message
        TRPCProtocol.TrpcStreamCloseMeta message = builder.build();

        // Test built message
        assertEquals(closeType, message.getCloseType());
        assertEquals(ret, message.getRet());
        assertEquals(msg, message.getMsg());
        assertEquals(messageType, message.getMessageType());
        assertEquals(transInfoValue, message.getTransInfoOrThrow("trpc-key"));
        assertEquals(funcRet, message.getFuncRet());

        // Test clear method
        builder.clear();
        assertEquals(0, builder.getCloseType());
        assertEquals(0, builder.getRet());
        assertEquals(ByteString.EMPTY, builder.getMsg());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getFuncRet());

        // Test mergeFrom method
        TRPCProtocol.TrpcStreamCloseMeta.Builder builder2 = TRPCProtocol.TrpcStreamCloseMeta.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(closeType, builder2.getCloseType());
        assertEquals(ret, builder2.getRet());
        assertEquals(msg, builder2.getMsg());
        assertEquals(messageType, builder2.getMessageType());
        assertEquals(transInfoValue, builder2.getTransInfoOrThrow("trpc-key"));
        assertEquals(funcRet, builder2.getFuncRet());
    }

    @Test
    public void testTrpcStreamCloseMetaParsing() throws Exception {
        // Create a new builder instance and set values
        TRPCProtocol.TrpcStreamCloseMeta.Builder builder = TRPCProtocol.TrpcStreamCloseMeta.newBuilder();
        int closeType = 1;
        int ret = 2;
        ByteString msg = ByteString.copyFromUtf8("Error message");
        int messageType = 3;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int funcRet = 4;

        builder.setCloseType(closeType);
        builder.setRet(ret);
        builder.setMsg(msg);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setFuncRet(funcRet);

        // Build the message
        TRPCProtocol.TrpcStreamCloseMeta message = builder.build();

        // Serialize the message to byte array
        byte[] data = message.toByteArray();

        // Parse the message from byte array
        TRPCProtocol.TrpcStreamCloseMeta parsedMessage = TRPCProtocol.TrpcStreamCloseMeta.parseFrom(data);

        // Test parsed message
        assertEquals(closeType, parsedMessage.getCloseType());
        assertEquals(ret, parsedMessage.getRet());
        assertEquals(msg, parsedMessage.getMsg());
        assertEquals(messageType, parsedMessage.getMessageType());
        assertEquals(transInfoValue, parsedMessage.getTransInfoOrThrow("trpc-key"));
        assertEquals(funcRet, parsedMessage.getFuncRet());
    }

    @Test
    public void testTrpcStreamCloseMetaEqualsAndHashCode() {
        // Create two builder instances and set values
        TRPCProtocol.TrpcStreamCloseMeta.Builder builder1 = TRPCProtocol.TrpcStreamCloseMeta.newBuilder();
        TRPCProtocol.TrpcStreamCloseMeta.Builder builder2 = TRPCProtocol.TrpcStreamCloseMeta.newBuilder();
        int closeType = 1;
        int ret = 2;
        ByteString msg = ByteString.copyFromUtf8("Error message");
        int messageType = 3;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int funcRet = 4;

        builder1.setCloseType(closeType);
        builder1.setRet(ret);
        builder1.setMsg(msg);
        builder1.setMessageType(messageType);
        builder1.putTransInfo("trpc-key", transInfoValue);
        builder1.setFuncRet(funcRet);

        builder2.setCloseType(closeType);
        builder2.setRet(ret);
        builder2.setMsg(msg);
        builder2.setMessageType(messageType);
        builder2.putTransInfo("trpc-key", transInfoValue);
        builder2.setFuncRet(funcRet);

        // Build the messages
        TRPCProtocol.TrpcStreamCloseMeta message1 = builder1.build();
        TRPCProtocol.TrpcStreamCloseMeta message2 = builder2.build();

        // Test equals and hashCode
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    public void testRequestProtocolDefaultInstance() {
        // Get the default instance
        TRPCProtocol.RequestProtocol defaultInstance = TRPCProtocol.RequestProtocol.getDefaultInstance();

        // Test default values
        assertEquals(0, defaultInstance.getVersion());
        assertEquals(0, defaultInstance.getCallType());
        assertEquals(0, defaultInstance.getRequestId());
        assertEquals(0, defaultInstance.getTimeout());
        assertEquals(ByteString.EMPTY, defaultInstance.getCaller());
        assertEquals(ByteString.EMPTY, defaultInstance.getCallee());
        assertEquals(ByteString.EMPTY, defaultInstance.getFunc());
        assertEquals(0, defaultInstance.getMessageType());
        assertTrue(defaultInstance.getTransInfoMap().isEmpty());
        assertEquals(0, defaultInstance.getContentType());
        assertEquals(0, defaultInstance.getContentEncoding());
        assertEquals(0, defaultInstance.getAttachmentSize());
    }

    @Test
    public void testRequestProtocolBuilder() {
        // Create a new builder instance
        TRPCProtocol.RequestProtocol.Builder builder = TRPCProtocol.RequestProtocol.newBuilder();

        // Test default values
        assertEquals(0, builder.getVersion());
        assertEquals(0, builder.getCallType());
        assertEquals(0, builder.getRequestId());
        assertEquals(0, builder.getTimeout());
        assertEquals(ByteString.EMPTY, builder.getCaller());
        assertEquals(ByteString.EMPTY, builder.getCallee());
        assertEquals(ByteString.EMPTY, builder.getFunc());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getContentType());
        assertEquals(0, builder.getContentEncoding());
        assertEquals(0, builder.getAttachmentSize());

        // Set values
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int timeout = 1000;
        ByteString caller = ByteString.copyFromUtf8("caller");
        ByteString callee = ByteString.copyFromUtf8("callee");
        ByteString func = ByteString.copyFromUtf8("func");
        int messageType = 4;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 5;
        int contentEncoding = 6;
        int attachmentSize = 7;

        builder.setVersion(version);
        builder.setCallType(callType);
        builder.setRequestId(requestId);
        builder.setTimeout(timeout);
        builder.setCaller(caller);
        builder.setCallee(callee);
        builder.setFunc(func);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setContentType(contentType);
        builder.setContentEncoding(contentEncoding);
        builder.setAttachmentSize(attachmentSize);

        // Test set values
        assertEquals(version, builder.getVersion());
        assertEquals(callType, builder.getCallType());
        assertEquals(requestId, builder.getRequestId());
        assertEquals(timeout, builder.getTimeout());
        assertEquals(caller, builder.getCaller());
        assertEquals(callee, builder.getCallee());
        assertEquals(func, builder.getFunc());
        assertEquals(messageType, builder.getMessageType());
        assertEquals(transInfoValue, builder.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, builder.getContentType());
        assertEquals(contentEncoding, builder.getContentEncoding());
        assertEquals(attachmentSize, builder.getAttachmentSize());

        // Build the message
        TRPCProtocol.RequestProtocol message = builder.build();

        // Test built message
        assertEquals(version, message.getVersion());
        assertEquals(callType, message.getCallType());
        assertEquals(requestId, message.getRequestId());
        assertEquals(timeout, message.getTimeout());
        assertEquals(caller, message.getCaller());
        assertEquals(callee, message.getCallee());
        assertEquals(func, message.getFunc());
        assertEquals(messageType, message.getMessageType());
        assertEquals(transInfoValue, message.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, message.getContentType());
        assertEquals(contentEncoding, message.getContentEncoding());
        assertEquals(attachmentSize, message.getAttachmentSize());

        // Test clear method
        builder.clear();
        assertEquals(0, builder.getVersion());
        assertEquals(0, builder.getCallType());
        assertEquals(0, builder.getRequestId());
        assertEquals(0, builder.getTimeout());
        assertEquals(ByteString.EMPTY, builder.getCaller());
        assertEquals(ByteString.EMPTY, builder.getCallee());
        assertEquals(ByteString.EMPTY, builder.getFunc());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getContentType());
        assertEquals(0, builder.getContentEncoding());
        assertEquals(0, builder.getAttachmentSize());

        // Test mergeFrom method
        TRPCProtocol.RequestProtocol.Builder builder2 = TRPCProtocol.RequestProtocol.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(version, builder2.getVersion());
        assertEquals(callType, builder2.getCallType());
        assertEquals(requestId, builder2.getRequestId());
        assertEquals(timeout, builder2.getTimeout());
        assertEquals(caller, builder2.getCaller());
        assertEquals(callee, builder2.getCallee());
        assertEquals(func, builder2.getFunc());
        assertEquals(messageType, builder2.getMessageType());
        assertEquals(transInfoValue, builder2.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, builder2.getContentType());
        assertEquals(contentEncoding, builder2.getContentEncoding());
        assertEquals(attachmentSize, builder2.getAttachmentSize());
    }

    @Test
    public void testRequestProtocolParsing() throws Exception {
        // Create a new builder instance and set values
        TRPCProtocol.RequestProtocol.Builder builder = TRPCProtocol.RequestProtocol.newBuilder();
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int timeout = 1000;
        ByteString caller = ByteString.copyFromUtf8("caller");
        ByteString callee = ByteString.copyFromUtf8("callee");
        ByteString func = ByteString.copyFromUtf8("func");
        int messageType = 4;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 5;
        int contentEncoding = 6;
        int attachmentSize = 7;

        builder.setVersion(version);
        builder.setCallType(callType);
        builder.setRequestId(requestId);
        builder.setTimeout(timeout);
        builder.setCaller(caller);
        builder.setCallee(callee);
        builder.setFunc(func);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setContentType(contentType);
        builder.setContentEncoding(contentEncoding);
        builder.setAttachmentSize(attachmentSize);

        // Build the message
        TRPCProtocol.RequestProtocol message = builder.build();

        // Serialize the message to byte array
        byte[] data = message.toByteArray();

        // Parse the message from byte array
        TRPCProtocol.RequestProtocol parsedMessage = TRPCProtocol.RequestProtocol.parseFrom(data);

        // Test parsed message
        assertEquals(version, parsedMessage.getVersion());
        assertEquals(callType, parsedMessage.getCallType());
        assertEquals(requestId, parsedMessage.getRequestId());
        assertEquals(timeout, parsedMessage.getTimeout());
        assertEquals(caller, parsedMessage.getCaller());
        assertEquals(callee, parsedMessage.getCallee());
        assertEquals(func, parsedMessage.getFunc());
        assertEquals(messageType, parsedMessage.getMessageType());
        assertEquals(transInfoValue, parsedMessage.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, parsedMessage.getContentType());
        assertEquals(contentEncoding, parsedMessage.getContentEncoding());
        assertEquals(attachmentSize, parsedMessage.getAttachmentSize());
    }

    @Test
    public void testRequestProtocolEqualsAndHashCode() {
        // Create two builder instances and set values
        TRPCProtocol.RequestProtocol.Builder builder1 = TRPCProtocol.RequestProtocol.newBuilder();
        TRPCProtocol.RequestProtocol.Builder builder2 = TRPCProtocol.RequestProtocol.newBuilder();
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int timeout = 1000;
        ByteString caller = ByteString.copyFromUtf8("caller");
        ByteString callee = ByteString.copyFromUtf8("callee");
        ByteString func = ByteString.copyFromUtf8("func");
        int messageType = 4;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 5;
        int contentEncoding = 6;
        int attachmentSize = 7;

        builder1.setVersion(version);
        builder1.setCallType(callType);
        builder1.setRequestId(requestId);
        builder1.setTimeout(timeout);
        builder1.setCaller(caller);
        builder1.setCallee(callee);
        builder1.setFunc(func);
        builder1.setMessageType(messageType);
        builder1.putTransInfo("trpc-key", transInfoValue);
        builder1.setContentType(contentType);
        builder1.setContentEncoding(contentEncoding);
        builder1.setAttachmentSize(attachmentSize);

        builder2.setVersion(version);
        builder2.setCallType(callType);
        builder2.setRequestId(requestId);
        builder2.setTimeout(timeout);
        builder2.setCaller(caller);
        builder2.setCallee(callee);
        builder2.setFunc(func);
        builder2.setMessageType(messageType);
        builder2.putTransInfo("trpc-key", transInfoValue);
        builder2.setContentType(contentType);
        builder2.setContentEncoding(contentEncoding);
        builder2.setAttachmentSize(attachmentSize);

        // Build the messages
        TRPCProtocol.RequestProtocol message1 = builder1.build();
        TRPCProtocol.RequestProtocol message2 = builder2.build();

        // Test equals and hashCode
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    public void testResponseProtocolDefaultInstance() {
        // Get the default instance
        TRPCProtocol.ResponseProtocol defaultInstance = TRPCProtocol.ResponseProtocol.getDefaultInstance();

        // Test default values
        assertEquals(0, defaultInstance.getVersion());
        assertEquals(0, defaultInstance.getCallType());
        assertEquals(0, defaultInstance.getRequestId());
        assertEquals(0, defaultInstance.getRet());
        assertEquals(0, defaultInstance.getFuncRet());
        assertEquals(ByteString.EMPTY, defaultInstance.getErrorMsg());
        assertEquals(0, defaultInstance.getMessageType());
        assertTrue(defaultInstance.getTransInfoMap().isEmpty());
        assertEquals(0, defaultInstance.getContentType());
        assertEquals(0, defaultInstance.getContentEncoding());
        assertEquals(0, defaultInstance.getAttachmentSize());
    }

    @Test
    public void testResponseProtocolBuilder() {
        // Create a new builder instance
        TRPCProtocol.ResponseProtocol.Builder builder = TRPCProtocol.ResponseProtocol.newBuilder();

        // Test default values
        assertEquals(0, builder.getVersion());
        assertEquals(0, builder.getCallType());
        assertEquals(0, builder.getRequestId());
        assertEquals(0, builder.getRet());
        assertEquals(0, builder.getFuncRet());
        assertEquals(ByteString.EMPTY, builder.getErrorMsg());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getContentType());
        assertEquals(0, builder.getContentEncoding());
        assertEquals(0, builder.getAttachmentSize());

        // Set values
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int ret = 4;
        int funcRet = 5;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");
        int messageType = 6;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 7;
        int contentEncoding = 8;
        int attachmentSize = 9;

        builder.setVersion(version);
        builder.setCallType(callType);
        builder.setRequestId(requestId);
        builder.setRet(ret);
        builder.setFuncRet(funcRet);
        builder.setErrorMsg(errorMsg);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setContentType(contentType);
        builder.setContentEncoding(contentEncoding);
        builder.setAttachmentSize(attachmentSize);

        // Test set values
        assertEquals(version, builder.getVersion());
        assertEquals(callType, builder.getCallType());
        assertEquals(requestId, builder.getRequestId());
        assertEquals(ret, builder.getRet());
        assertEquals(funcRet, builder.getFuncRet());
        assertEquals(errorMsg, builder.getErrorMsg());
        assertEquals(messageType, builder.getMessageType());
        assertEquals(transInfoValue, builder.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, builder.getContentType());
        assertEquals(contentEncoding, builder.getContentEncoding());
        assertEquals(attachmentSize, builder.getAttachmentSize());

        // Build the message
        TRPCProtocol.ResponseProtocol message = builder.build();

        // Test built message
        assertEquals(version, message.getVersion());
        assertEquals(callType, message.getCallType());
        assertEquals(requestId, message.getRequestId());
        assertEquals(ret, message.getRet());
        assertEquals(funcRet, message.getFuncRet());
        assertEquals(errorMsg, message.getErrorMsg());
        assertEquals(messageType, message.getMessageType());
        assertEquals(transInfoValue, message.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, message.getContentType());
        assertEquals(contentEncoding, message.getContentEncoding());
        assertEquals(attachmentSize, message.getAttachmentSize());

        // Test clear method
        builder.clear();
        assertEquals(0, builder.getVersion());
        assertEquals(0, builder.getCallType());
        assertEquals(0, builder.getRequestId());
        assertEquals(0, builder.getRet());
        assertEquals(0, builder.getFuncRet());
        assertEquals(ByteString.EMPTY, builder.getErrorMsg());
        assertEquals(0, builder.getMessageType());
        assertTrue(builder.getTransInfoMap().isEmpty());
        assertEquals(0, builder.getContentType());
        assertEquals(0, builder.getContentEncoding());
        assertEquals(0, builder.getAttachmentSize());

        // Test mergeFrom method
        TRPCProtocol.ResponseProtocol.Builder builder2 = TRPCProtocol.ResponseProtocol.newBuilder();
        builder2.mergeFrom(message);
        assertEquals(version, builder2.getVersion());
        assertEquals(callType, builder2.getCallType());
        assertEquals(requestId, builder2.getRequestId());
        assertEquals(ret, builder2.getRet());
        assertEquals(funcRet, builder2.getFuncRet());
        assertEquals(errorMsg, builder2.getErrorMsg());
        assertEquals(messageType, builder2.getMessageType());
        assertEquals(transInfoValue, builder2.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, builder2.getContentType());
        assertEquals(contentEncoding, builder2.getContentEncoding());
        assertEquals(attachmentSize, builder2.getAttachmentSize());
    }

    @Test
    public void testResponseProtocolParsing() throws Exception {
        // Create a new builder instance and set values
        TRPCProtocol.ResponseProtocol.Builder builder = TRPCProtocol.ResponseProtocol.newBuilder();
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int ret = 4;
        int funcRet = 5;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");
        int messageType = 6;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 7;
        int contentEncoding = 8;
        int attachmentSize = 9;

        builder.setVersion(version);
        builder.setCallType(callType);
        builder.setRequestId(requestId);
        builder.setRet(ret);
        builder.setFuncRet(funcRet);
        builder.setErrorMsg(errorMsg);
        builder.setMessageType(messageType);
        builder.putTransInfo("trpc-key", transInfoValue);
        builder.setContentType(contentType);
        builder.setContentEncoding(contentEncoding);
        builder.setAttachmentSize(attachmentSize);

        // Build the message
        TRPCProtocol.ResponseProtocol message = builder.build();

        // Serialize the message to byte array
        byte[] data = message.toByteArray();

        // Parse the message from byte array
        TRPCProtocol.ResponseProtocol parsedMessage = TRPCProtocol.ResponseProtocol.parseFrom(data);

        // Test parsed message
        assertEquals(version, parsedMessage.getVersion());
        assertEquals(callType, parsedMessage.getCallType());
        assertEquals(requestId, parsedMessage.getRequestId());
        assertEquals(ret, parsedMessage.getRet());
        assertEquals(funcRet, parsedMessage.getFuncRet());
        assertEquals(errorMsg, parsedMessage.getErrorMsg());
        assertEquals(messageType, parsedMessage.getMessageType());
        assertEquals(transInfoValue, parsedMessage.getTransInfoOrThrow("trpc-key"));
        assertEquals(contentType, parsedMessage.getContentType());
        assertEquals(contentEncoding, parsedMessage.getContentEncoding());
        assertEquals(attachmentSize, parsedMessage.getAttachmentSize());
    }

    @Test
    public void testResponseProtocolEqualsAndHashCode() {
        // Create two builder instances and set values
        TRPCProtocol.ResponseProtocol.Builder builder1 = TRPCProtocol.ResponseProtocol.newBuilder();
        TRPCProtocol.ResponseProtocol.Builder builder2 = TRPCProtocol.ResponseProtocol.newBuilder();
        int version = 1;
        int callType = 2;
        int requestId = 3;
        int ret = 4;
        int funcRet = 5;
        ByteString errorMsg = ByteString.copyFromUtf8("Error message");
        int messageType = 6;
        ByteString transInfoValue = ByteString.copyFromUtf8("value");
        int contentType = 7;
        int contentEncoding = 8;
        int attachmentSize = 9;

        builder1.setVersion(version);
        builder1.setCallType(callType);
        builder1.setRequestId(requestId);
        builder1.setRet(ret);
        builder1.setFuncRet(funcRet);
        builder1.setErrorMsg(errorMsg);
        builder1.setMessageType(messageType);
        builder1.putTransInfo("trpc-key", transInfoValue);
        builder1.setContentType(contentType);
        builder1.setContentEncoding(contentEncoding);
        builder1.setAttachmentSize(attachmentSize);

        builder2.setVersion(version);
        builder2.setCallType(callType);
        builder2.setRequestId(requestId);
        builder2.setRet(ret);
        builder2.setFuncRet(funcRet);
        builder2.setErrorMsg(errorMsg);
        builder2.setMessageType(messageType);
        builder2.putTransInfo("trpc-key", transInfoValue);
        builder2.setContentType(contentType);
        builder2.setContentEncoding(contentEncoding);
        builder2.setAttachmentSize(attachmentSize);

        // Build the messages
        TRPCProtocol.ResponseProtocol message1 = builder1.build();
        TRPCProtocol.ResponseProtocol message2 = builder2.build();

        // Test equals and hashCode
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }
}
