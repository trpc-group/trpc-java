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

package com.tencent.trpc.codegen.protobuf;

import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.tencent.trpc.codegen.CodegenTestHelper;
import com.tencent.trpc.codegen.protoc.ProtocTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DescriptorProtoCompileTest {
    private static final Path rootPath = CodegenTestHelper.TEST_ROOT;

    @BeforeClass
    public static void prepare() {
        Path descriptorFile = rootPath.resolve("hello.pb");
        if (!Files.exists(descriptorFile)) {
            ProtocTest protocTest = new ProtocTest();
            protocTest.testGenerateDescriptorSet();
        }
    }

    @Test
    public void testParseProtoDescriptorSet() throws IOException {
        Path descriptorFile = rootPath.resolve("hello.pb");
        FileDescriptorSet set = ProtoParser.parseDescriptorSetFile(descriptorFile);
        Assert.assertEquals(5, set.getFileCount());
        FileDescriptorsCompiler compiler = new FileDescriptorsCompiler(set.getFileList());
        List<FileDescriptor> fdList = compiler.compile();
        Assert.assertEquals(5, fdList.size());
        ProtoSourceInfo sourceInfo = ProtoParser.parseFileDescriptors(fdList);
        Assert.assertEquals(1, sourceInfo.getServices().size());
    }
}
