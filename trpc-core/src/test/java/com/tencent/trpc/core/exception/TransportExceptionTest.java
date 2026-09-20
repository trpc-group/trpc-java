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

package com.tencent.trpc.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UnknownFormatConversionException;
import org.junit.jupiter.api.Test;

/**
 * Test {@link TransportException}, especially the {@code create} methods which format the message by themselves, so
 * the callers must pass the raw format and the arguments instead of an already formatted string.
 */
public class TransportExceptionTest {

    private static final String MESSAGE = "transport error";

    @Test
    public void testConstructWithMessage() {
        TransportException e = new TransportException(MESSAGE);
        assertEquals(MESSAGE, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    public void testConstructWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("cause");
        TransportException e = new TransportException(MESSAGE, cause);
        assertEquals(MESSAGE, e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void testConstructWithCause() {
        IllegalStateException cause = new IllegalStateException("cause");
        TransportException e = new TransportException(cause);
        assertEquals("cause", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void testCreateFormatsArguments() {
        TransportException e = TransportException.create("send fail, addr=%s, port=%s", "127.0.0.1", 8080);
        assertEquals("send fail, addr=127.0.0.1, port=8080", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    public void testCreateWithCauseFormatsArguments() {
        IllegalStateException cause = new IllegalStateException("cause");
        TransportException e = TransportException.create(cause, "send fail, addr=%s", "127.0.0.1");
        assertEquals("send fail, addr=127.0.0.1", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void testCreateWithNullCauseFormatsArguments() {
        TransportException e = TransportException.create((Throwable) null, "send fail, addr=%s", "127.0.0.1");
        assertEquals("send fail, addr=127.0.0.1", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    public void testCreateWithoutArguments() {
        TransportException e = TransportException.create("send fail");
        assertEquals("send fail", e.getMessage());
    }

    @Test
    public void testCreateWithNullArgument() {
        TransportException e = TransportException.create("send fail, msg=%s", (Object) null);
        assertEquals("send fail, msg=null", e.getMessage());
    }

    /**
     * The argument may contain a percent sign, and it must be kept as it is, because only the format is formatted.
     */
    @Test
    public void testCreateKeepsPercentSignOfArgument() {
        TransportException e = TransportException.create("send fail, msg=%s", "GET /a%2Fb?rate=100%");
        assertEquals("send fail, msg=GET /a%2Fb?rate=100%", e.getMessage());
    }

    /**
     * The caller must never pass an already formatted message which contains a percent sign, otherwise the message
     * is formatted twice and the real error is hidden by a format exception. This test pins down the behaviour so
     * that the double formatting can be caught.
     */
    @Test
    public void testCreateWithAlreadyFormattedMessageContainingPercentFails() {
        String formatted = String.format("send fail, msg=%s", "GET /a%2Fb");
        assertEquals("send fail, msg=GET /a%2Fb", formatted);
        try {
            TransportException.create(formatted);
            fail("UnknownFormatConversionException is expected");
        } catch (UnknownFormatConversionException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testTransReturnsTheSameInstanceForTransportException() {
        TransportException origin = new TransportException(MESSAGE);
        assertSame(origin, TransportException.trans(origin));
        assertSame(origin, TransportException.trans(origin, "other message"));
    }

    @Test
    public void testTransWrapsOtherException() {
        IllegalStateException cause = new IllegalStateException("cause");
        TransportException e = TransportException.trans(cause);
        assertEquals("cause", e.getMessage());
        assertSame(cause, e.getCause());

        TransportException withMessage = TransportException.trans(cause, MESSAGE);
        assertEquals(MESSAGE, withMessage.getMessage());
        assertSame(cause, withMessage.getCause());
    }

    @Test
    public void testIsRuntimeException() {
        assertTrue(new TransportException(MESSAGE) instanceof RuntimeException);
    }
}
