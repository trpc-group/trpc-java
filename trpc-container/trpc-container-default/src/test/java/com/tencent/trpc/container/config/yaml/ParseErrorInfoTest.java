package com.tencent.trpc.container.config.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ParseErrorInfoTest {

    @Test
    public void testInfo() {
        ParseErrorInfo parseErrorInfo = new ParseErrorInfo();
        Assertions.assertNotNull(parseErrorInfo);
        String info = ParseErrorInfo.info("key", "value");
        Assertions.assertNotNull(info);
    }

    @Test
    public void testTestInfo() {
        String info = ParseErrorInfo.info("key", "index", "value");
        Assertions.assertNotNull(info);
    }
}
