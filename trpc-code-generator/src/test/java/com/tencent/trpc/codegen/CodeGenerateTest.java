/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.codegen;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.tencent.trpc.codegen.protoc.Language;
import com.tencent.trpc.codegen.protoc.Protoc;
import com.tencent.trpc.codegen.protoc.ProtocPlugin;
import com.tencent.trpc.codegen.template.CodeFileGenerator;
import com.tencent.trpc.codegen.template.CodeScope;
import com.tencent.trpc.codegen.template.CodeTemplate;
import com.tencent.trpc.codegen.template.CodeType;
import com.tencent.trpc.codegen.template.DefaultCodeTemplates;
import com.tencent.trpc.codegen.template.FreeMarkerContextProvider;
import com.tencent.trpc.codegen.template.FreeMarkerStringTemplateEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

public class CodeGenerateTest {

    private static final Path rootPath = CodegenTestHelper.TEST_ROOT;
    private static final String protocExecutable = CodegenTestHelper.PROTOC_EXECUTABLE;
    private static final String protocGenValidateExecutable = CodegenTestHelper.PROTOC_GEN_VALIDATE_EXECUTABLE;

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-1
     * <p></p>Case: standard proto file with non-stream api
     */
    @Test
    public void testTRpcCase1() throws IOException {
        runTest(Protocol.TRPC, "TEST-1");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("6eaef685e6b986ce56e3bcc7d14e1c4c",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("23446b6413c80bf8a1aa6ee234c6fba9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("9caa57d08d0b3fcb9d617ac8d8d135e5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-1
     * <p></p>Case: standard proto file with non-stream api
     */
    @Test
    public void testGRpcCase1() throws IOException {
        runTest(Protocol.GRPC, "TEST-1");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("7c9a444f814fb2881053aaff9d227db6",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("23446b6413c80bf8a1aa6ee234c6fba9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("9caa57d08d0b3fcb9d617ac8d8d135e5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-2
     * <p></p>Case: standard proto file with stream api
     */
    @Test
    public void testTRpcCase2() throws IOException {
        runTest(Protocol.TRPC, "TEST-2");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("7c9944d4b57d3c5bcf0e0e41e543322d",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("075e5962815321ae423bc38165fdf303",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("285ef96d563b7b5f87472ebb218f1624",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("2eb8ad08c973e8cb107cc596db1709b1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-2
     * <p></p>Case: standard proto file with stream api
     */
    @Test
    public void testGRpcCase2() throws IOException {
        runTest(Protocol.GRPC, "TEST-2");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("c61eda56e21950f11615ac0a6c2d5fe8",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("075e5962815321ae423bc38165fdf303",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("be4038417c2098b60a7af42e73c1abf6",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("2eb8ad08c973e8cb107cc596db1709b1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-3
     * <p></p>Case: proto file with stream api and imported local proto file
     */
    @Test
    public void testTRpcCase3() throws IOException {
        runTest(Protocol.TRPC, "TEST-3");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("c817495291050d99de03589c0b2840bf",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("b8cd07293d73ebb299f1997136b9cfd6",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("926fcc803803125fc0910f4b7c309b26",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("9feac2c00a6b5fb137e0347e9d3c57a9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
        Assert.assertEquals("fa93e09f70c157d59019dd62610cb4f3",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("msg").resolve("Hello.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-3
     * <p></p>Case: proto file with stream api and imported local proto file
     */
    @Test
    public void testGRpcCase3() throws IOException {
        runTest(Protocol.GRPC, "TEST-3");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("a3bcaeb3ec937819eb26fe3a60219957",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("b8cd07293d73ebb299f1997136b9cfd6",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("e8108341efbc3d36bf3dcc9ba8ea0b41",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("9feac2c00a6b5fb137e0347e9d3c57a9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
        Assert.assertEquals("fa93e09f70c157d59019dd62610cb4f3",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("msg").resolve("Hello.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-4
     * <p></p>Case: java_multiple_files = true and snake case filename
     */
    @Test
    public void testTRpcCase4() throws IOException {
        runTest(Protocol.TRPC, "TEST-4");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("trpc", "exp", "helloworld"));
        Assert.assertEquals("b5e67d899ef1885461d791a22631e49b",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("b7640a2fe62a3464ad25d0be603d1966",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("608f0a195f7e542aa11be2ff5fb82199",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloRequest.java"))));
        Assert.assertEquals("01e5a8f2f794ee7c81873fc62c93c31a",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloRequestOrBuilder.java"))));
        Assert.assertEquals("d24f652118fcd17e03b65c7423b71fbf",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloReply.java"))));
        Assert.assertEquals("d0341d2cdbeaca4557c25e6f07c65458",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloReplyOrBuilder.java"))));
        Assert.assertEquals("cbea0bc31f8ce927120b77c4b26475d5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloNext.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-4
     * <p></p>Case: java_multiple_files = true and snake case filename
     */
    @Test
    public void testGRpcCase4() throws IOException {
        runTest(Protocol.GRPC, "TEST-4");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("trpc", "exp", "helloworld"));
        Assert.assertEquals("154e3b53d0f034155ff1603998a105a5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("b7640a2fe62a3464ad25d0be603d1966",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("608f0a195f7e542aa11be2ff5fb82199",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloRequest.java"))));
        Assert.assertEquals("01e5a8f2f794ee7c81873fc62c93c31a",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloRequestOrBuilder.java"))));
        Assert.assertEquals("d24f652118fcd17e03b65c7423b71fbf",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloReply.java"))));
        Assert.assertEquals("d0341d2cdbeaca4557c25e6f07c65458",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloReplyOrBuilder.java"))));
        Assert.assertEquals("cbea0bc31f8ce927120b77c4b26475d5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloNext.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-5
     * <p></p>Case: with validate rules
     */
    @Test
    public void testTRpcCase5() throws IOException {
        runTest(Protocol.TRPC, "TEST-5");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test", "helloworld"));
        Assert.assertEquals("c0ac8b85e80f949dd9674a31d2fb28c1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("3d0ff8da3ace15f1bd055164ec1da4b9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("b23d158eedf9ea3912703f084ff72ec2",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
        Assert.assertEquals("9277fefe3ff6af9a95062d359db14ebe",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceValidator.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-5
     * <p></p>Case: with validate rules
     */
    @Test
    public void testGRpcCase5() throws IOException {
        runTest(Protocol.GRPC, "TEST-5");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test", "helloworld"));
        Assert.assertEquals("53541c1d2a78e70b8e0b7323b77721bf",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("3d0ff8da3ace15f1bd055164ec1da4b9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("b23d158eedf9ea3912703f084ff72ec2",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
        Assert.assertEquals("9277fefe3ff6af9a95062d359db14ebe",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceValidator.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-6
     * <p></p>Case: java_multiple_files = false, java_outer_classname not specified, snake case filename
     */
    @Test
    public void testTRpcCase6() throws IOException {
        runTest(Protocol.TRPC, "TEST-6");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("0e292fa89a93a0cbd3ecb51d3ca42b31",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("70abf7c48136e3f8d2a08ec15796a47e",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("2f519ad1efafb3d145c5847e7831b559",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("62ed6dc1ad9883094281a4bc35416c56",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloCamelCase.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-6
     * <p></p>Case: java_multiple_files = false, java_outer_classname not specified, snake case filename
     */
    @Test
    public void testGRpcCase6() throws IOException {
        runTest(Protocol.GRPC, "TEST-6");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("b0bdf649fa7269a0160846d5302952be",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("70abf7c48136e3f8d2a08ec15796a47e",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("651a3065ac06e2b157ef7b8ba15bce6a",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("62ed6dc1ad9883094281a4bc35416c56",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloCamelCase.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-7
     * <p></p>Case: snake case service name
     */
    @Test
    public void testTRpcCase7() throws IOException {
        runTest(Protocol.TRPC, "TEST-7");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("a5fe0f6823f80d27574e2051da1ed33e",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceAPI.java"))));
        Assert.assertEquals("33b88906768a6b7c8b665714dfda8bd1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceAsyncAPI.java"))));
        Assert.assertEquals("777387facf2cbc71228c30de20ed2226",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceStreamAPI.java"))));
        Assert.assertEquals("640d503c66e105c9776037895a406613",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-6
     * <p></p>Case: snake case service name
     */
    @Test
    public void testGRpcCase7() throws IOException {
        runTest(Protocol.GRPC, "TEST-7");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("b7b0a488c9e5b4c2f3d789a7e9833e8a",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceAPI.java"))));
        Assert.assertEquals("33b88906768a6b7c8b665714dfda8bd1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceAsyncAPI.java"))));
        Assert.assertEquals("a0c28a7babedf863e31740b320857b0f",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterServiceStreamAPI.java"))));
        Assert.assertEquals("640d503c66e105c9776037895a406613",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol TRPC on src/test/resources/TEST-8
     * <p></p>Case: camel case service name
     */
    @Test
    public void testTRpcCase8() throws IOException {
        runTest(Protocol.TRPC, "TEST-8");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("2d9780f352044c82bfef69afebeb7da5",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceAPI.java"))));
        Assert.assertEquals("4ed1fab5faa4a31c54aa452e54664853",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceAsyncAPI.java"))));
        Assert.assertEquals("446ff8e1c7479bb3f7da2d2c61cdf004",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceStreamAPI.java"))));
        Assert.assertEquals("639e78687054ee0ae8a88749fc3f36c3",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    /**
     * Run code-generation with protocol GRPC on src/test/resources/TEST-8
     * <p></p>Case: camel case service name
     */
    @Test
    public void testGRpcCase8() throws IOException {
        runTest(Protocol.GRPC, "TEST-8");
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("0cb047dba4e1726c8ac909447962e251",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceAPI.java"))));
        Assert.assertEquals("4ed1fab5faa4a31c54aa452e54664853",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceAsyncAPI.java"))));
        Assert.assertEquals("8e86f7527ddb01d27b574c6de39270d1",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("HelloWorldServiceStreamAPI.java"))));
        Assert.assertEquals("639e78687054ee0ae8a88749fc3f36c3",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
    }

    @Test
    public void testHookedCodeGenerate() throws IOException {
        List<CodeTemplate<String>> templates = new ArrayList<>(DefaultCodeTemplates.getInstance().getCodeTemplates());
        templates.add(new CodeTemplate<>("custom", "${custom.key1}",
                CodeType.CUSTOM, CodeScope.SERVICE, "%sCustom.txt"));
        TRpcCodeGenerator generator =
                TRpcCodeGenerator.builder()
                        .codeFileGenerator(CodeFileGenerator.<String, Map<String, Object>>builder()
                                .codeTemplateEngine(new FreeMarkerStringTemplateEngine())
                                .templateContextProvider(new FreeMarkerContextProvider())
                                .codeTemplates(templates)
                                .build())
                        .protocol(Protocol.TRPC)
                        .language(Language.JAVA)
                        .protoc(new Protoc(protocExecutable))
                        .protoPath(rootPath.resolve("TEST-3"))
                        .outPath(Paths.get("target", "generated-sources", "trpc").toAbsolutePath())
                        .codeGeneratorHook(new TRpcCodeGeneratorHook() {
                            @Override
                            public List<Path> getAdditionalProtoDependencyPaths() {
                                return Collections.emptyList();
                            }

                            @Override
                            public Map<String, Object> getCustomVariables(List<FileDescriptor> fileDescriptors) {
                                return ImmutableMap.of("key1", "abcd");
                            }
                        })
                        .build();
        generator.generateCode();
        Path base = getOutputBasePath();
        Path java = base.resolve(Paths.get("com", "tencent", "trpc", "codegen", "test"));
        Assert.assertEquals("c817495291050d99de03589c0b2840bf",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAPI.java"))));
        Assert.assertEquals("b8cd07293d73ebb299f1997136b9cfd6",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterAsyncAPI.java"))));
        Assert.assertEquals("926fcc803803125fc0910f4b7c309b26",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterStreamAPI.java"))));
        Assert.assertEquals("9feac2c00a6b5fb137e0347e9d3c57a9",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("GreeterService.java"))));
        Assert.assertEquals("fa93e09f70c157d59019dd62610cb4f3",
                DigestUtils.md5Hex(Files.readAllBytes(java.resolve("msg").resolve("Hello.java"))));
    }

    private void runTest(Protocol protocol, String path) {
        TRpcCodeGenerator.builder()
                .codeFileGenerator(CodeFileGenerator.createDefault())
                .protocol(protocol)
                .language(Language.JAVA)
                .protoc(new Protoc(protocExecutable,
                        ImmutableMap.of(ProtocPlugin.GEN_VALIDATE_PLUGIN_NAME,
                                new ProtocPlugin(ProtocPlugin.GEN_VALIDATE_PLUGIN_NAME, protocGenValidateExecutable))))
                .protoPath(rootPath.resolve(path))
                .outPath(Paths.get("target", "generated-sources", "trpc").toAbsolutePath())
                .build()
                .generateCode();
    }

    private Path getOutputBasePath() {
        return Paths.get("target", "generated-sources", "trpc").toAbsolutePath();
    }
}