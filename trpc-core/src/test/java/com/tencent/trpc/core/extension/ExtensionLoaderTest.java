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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.tests.DefaultNotExistExt;
import com.tencent.trpc.core.extension.tests.NoAnnotationExt;
import com.tencent.trpc.core.extension.tests.ext1.Ext1;
import com.tencent.trpc.core.extension.tests.ext1.impl.Ext1Impl1;
import com.tencent.trpc.core.extension.tests.ext1.impl.Ext1Impl2;
import com.tencent.trpc.core.extension.tests.ext1.impl.Ext1Impl3;
import com.tencent.trpc.core.extension.tests.ext2.Ext2;
import com.tencent.trpc.core.extension.tests.ext3.Ext3;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl1;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl5;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl6;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl7;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl8;
import com.tencent.trpc.core.extension.tests.ext3.impl.Ext3Impl9;
import com.tencent.trpc.core.extension.tests.ext4.Ext4;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.selector.support.ip.IpSelector;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * NOTE: Due to the fact that ExtensionLoader is cached through static variables,
 * it is important to be careful not to interfere with each other during testing.
 */
public class ExtensionLoaderTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testGetExtensionLoaderNoExtensibleAnnotation() {
        expectedEx.expect(TRpcExtensionException.class);
        expectedEx.expectMessage("does not annotated with");
        ExtensionLoader.getExtensionLoader(NoAnnotationExt.class);
    }

    @Test
    public void testGetAllExtension() {
        List<Ext1> allExtensions = ExtensionLoader.getExtensionLoader(Ext1.class).getAllExtensions();
        Assert.assertNotNull(allExtensions);
    }

    @Test
    public void testAddExtension() {
        ExtensionLoader<Ext1> extensionLoader1 = ExtensionLoader.getExtensionLoader(Ext1.class);
        extensionLoader1.addExtension("impl1", Ext1Impl1.class);
        extensionLoader1.addExtension("impl2", Ext1Impl2.class);
        Ext1 impl1 = extensionLoader1.getExtension("impl1");
        Ext1 impl2 = extensionLoader1.getExtension("impl2");
        Assert.assertTrue(impl1 instanceof Ext1Impl1);
        Assert.assertTrue(impl2 instanceof Ext1Impl2);
        ExtensionLoader extensionLoader2 = ExtensionLoader.getExtensionLoader(Ext1.class);
        Assert.assertSame(extensionLoader1, extensionLoader2);
        Assert.assertSame(impl1, extensionLoader2.getExtension("impl1"));
        Assert.assertSame(impl2, extensionLoader2.getExtension("impl2"));
    }

    @Test
    public void testAddExtensionExist() {
        expectedEx.expect(TRpcExtensionException.class);
        expectedEx.expectMessage("duplicate extension name");
        TRpcSystemProperties.setIgnoreSamePluginName(Boolean.FALSE);
        ExtensionLoader<Ext1> extensionLoader = ExtensionLoader.getExtensionLoader(Ext1.class);
        extensionLoader.addExtension("implExist", Ext1Impl1.class);
        extensionLoader.addExtension("implExist", Ext1Impl2.class);
    }

    @Test
    public void testExtensionManager() {
        ExtensionLoader<Ext1> extensionLoader = ExtensionLoader.getExtensionLoader(Ext1.class);
        extensionLoader.addExtension("implExist", Ext1Impl1.class);
        ExtensionManager<Ext1> ext1ExtensionManager = new ExtensionManager<>(Ext1.class);
        Assert.assertNull(ext1ExtensionManager.getDefaultExtension());
        Assert.assertNull(ext1ExtensionManager.getConfig("implExist"));
    }

    @Test
    public void testAddExtensionSpecify2Names() {
        Extension annotation = Ext1Impl3.class.getAnnotation(Extension.class);
        Assert.assertNotNull(annotation);
        String name = "implDuplicate";
        String nameOnClass = annotation.value();
        Assert.assertNotEquals(name, nameOnClass);
        ExtensionLoader<Ext1> extensionLoader = ExtensionLoader.getExtensionLoader(Ext1.class);
        extensionLoader.addExtension(name, Ext1Impl3.class);
        Assert.assertNotNull(extensionLoader.getExtension(name));
        expectedEx.expect(TRpcExtensionException.class);
        expectedEx.expectMessage("Cannot get extension");
        extensionLoader.getExtension(nameOnClass);
    }

    @Test
    public void testRemoveExtension() {
        ExtensionLoader<Ext1> extensionLoader1 = ExtensionLoader.getExtensionLoader(Ext1.class);
        extensionLoader1.addExtension("impl1", Ext1Impl1.class);
        Assert.assertNotNull(extensionLoader1.getExtension("impl1"));
        extensionLoader1.removeExtension("impl1");
        try {
            extensionLoader1.getExtension("impl1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHasExtension() {
        // Clear the plugin cache first.
        try {
            Field loaderMapper = ExtensionLoader.class.getDeclaredField("LOADER_MAPPER");
            loaderMapper.setAccessible(true);
            ConcurrentMap<Class<?>, ExtensionLoader<?>> loaders =
                    (ConcurrentMap<Class<?>, ExtensionLoader<?>>) loaderMapper.get(null);
            loaders.remove(Selector.class);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("clean extensions cache failed");
        }
        Assert.assertFalse(ExtensionLoader.hasExtensionLoader(Selector.class));
        ExtensionLoader.getExtensionLoader(Selector.class);
        Assert.assertTrue(ExtensionLoader.hasExtensionLoader(Selector.class));
    }

    @Test
    public void testReplaceExtension() {
        ExtensionLoader<Selector> extensionLoader = ExtensionLoader
                .getExtensionLoader(Selector.class);
        extensionLoader.addExtension("selector", IpSelector.class);
        Assert.assertTrue(extensionLoader.getExtension("selector") instanceof IpSelector);
        extensionLoader.replaceExtension("selector", MockSelector.class);
        Assert.assertTrue(extensionLoader.getExtension("selector") instanceof MockSelector);
    }

    @Test
    public void testRefreshExtension() {
        ExtensionLoader<Selector> extensionLoader = ExtensionLoader
                .getExtensionLoader(Selector.class);
        extensionLoader.addExtension("selector1", IpSelector.class);
        Assert.assertTrue(extensionLoader.getExtension("selector1") instanceof IpSelector);
        PluginConfig pluginConfig = new PluginConfig("selector1", MockSelector.class);
        try {
            extensionLoader.refresh("selector1", pluginConfig);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TRpcExtensionException);
        }
        ExtensionClass<Selector> selector = extensionLoader.getExtensionClass("selector1");
        selector.refresh(pluginConfig);
    }

    @Test
    public void testGetDefaultExtension() {
        ExtensionLoader<Ext2> extensionLoader = ExtensionLoader.getExtensionLoader(Ext2.class);
        Assert.assertNotNull(extensionLoader.getDefaultExtension());
    }

    @Test
    public void testGetDefaultExtensionNotExist() {
        // default plugins have been not configured.
        ExtensionLoader<Ext1> extensionLoader1 = ExtensionLoader.getExtensionLoader(Ext1.class);
        Assert.assertNull(extensionLoader1.getDefaultExtension());
        // The default plugin configured does not exist.
        ExtensionLoader<DefaultNotExistExt> extensionLoader2 =
                ExtensionLoader.getExtensionLoader(DefaultNotExistExt.class);
        Assert.assertNull(extensionLoader2.getDefaultExtension());
    }

    @Test
    public void testGetExtensionWithConfigFile() {
        ExtensionLoader<Ext3> extensionLoader = ExtensionLoader.getExtensionLoader(Ext3.class);
        Assert.assertNotNull(extensionLoader.getExtension("impl1"));
        Assert.assertNotNull(extensionLoader.getExtension("impl2"));
        Assert.assertNotNull(extensionLoader.getExtension("impl3"));
        Assert.assertNotNull(extensionLoader.getExtension("impl4"));
        Assert.assertNotNull(extensionLoader.getExtension("impl5"));
        Assert.assertNotNull(extensionLoader.getExtension("impl6"));
        Assert.assertNotNull(extensionLoader.getExtension("impl7"));
        Assert.assertNotNull(extensionLoader.getExtension("impl8"));
        Assert.assertNotNull(extensionLoader.getExtension("impl9"));
        try {
            extensionLoader.getExtension("impl10");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TRpcExtensionException);
        }
        Assert.assertSame(extensionLoader.getExtension("impl1"),
                extensionLoader.getExtension("impl1"));
        Assert.assertSame(extensionLoader.getExtension("impl3"),
                extensionLoader.getExtension("impl3"));
        Assert.assertSame(extensionLoader.getExtension("impl5"),
                extensionLoader.getExtension("impl5"));
        Assert.assertSame(extensionLoader.getExtension("impl6"),
                extensionLoader.getExtension("impl6"));
        Assert.assertSame(extensionLoader.getExtension("impl7"),
                extensionLoader.getExtension("impl7"));
        Assert.assertSame(extensionLoader.getExtension("impl9"),
                extensionLoader.getExtension("impl9"));
    }

    @Test
    public void testGetExtensionWithPluginConfig() {
        ExtensionLoader<Ext3> extensionLoader = ExtensionLoader.getExtensionLoader(Ext3.class);
        try {
            extensionLoader.getExtension("impl10");
        } catch (Exception ex) {
            Assert.assertEquals(ex.getCause().getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testGetActivateExtensions() {
        ExtensionLoader<Ext3> extensionLoader = ExtensionLoader.getExtensionLoader(Ext3.class);
        List<Ext3> providerExtensions = extensionLoader
                .getActivateExtensions(ActivationGroup.PROVIDER);
        List<Ext3> consumerExtensions = extensionLoader
                .getActivateExtensions(ActivationGroup.CONSUMER);
        List<Class<? extends Ext3>> providerExtensionClasses =
                Arrays.asList(Ext3Impl9.class, Ext3Impl5.class, Ext3Impl7.class, Ext3Impl8.class);
        List<Class<? extends Ext3>> consumerExtensionClasses =
                Arrays.asList(Ext3Impl9.class, Ext3Impl6.class, Ext3Impl7.class, Ext3Impl8.class);
        assertExtensionClassesEqual(providerExtensions, providerExtensionClasses);
        assertExtensionClassesEqual(consumerExtensions, consumerExtensionClasses);
    }

    private <T> void assertExtensionClassesEqual(List<T> extensions,
            List<Class<? extends T>> extensionClasses) {
        Assert.assertNotNull(extensions);
        Assert.assertNotNull(extensionClasses);
        Iterator<T> extIter = extensions.iterator();
        Iterator<Class<? extends T>> extClassIter = extensionClasses.iterator();
        while (extIter.hasNext() && extClassIter.hasNext()) {
            T ext = extIter.next();
            Class<? extends T> extClass = extClassIter.next();
            Assert.assertNotNull(ext);
            Assert.assertNotNull(extClass);
            Assert.assertSame(ext.getClass(), extClass);
        }
        Assert.assertFalse(extIter.hasNext());
        Assert.assertFalse(extClassIter.hasNext());
    }

    @Test
    public void testGetActivateExtensionsNotExist() {
        ExtensionLoader<DefaultNotExistExt> extensionLoader1 =
                ExtensionLoader.getExtensionLoader(DefaultNotExistExt.class);
        Assert.assertTrue(extensionLoader1.getActivateExtensions(ActivationGroup.PROVIDER).isEmpty());
        Assert.assertTrue(extensionLoader1.getActivateExtensions(ActivationGroup.CONSUMER).isEmpty());
        ExtensionLoader<Ext2> extensionLoader2 = ExtensionLoader.getExtensionLoader(Ext2.class);
        Assert.assertTrue(extensionLoader2.getActivateExtensions(ActivationGroup.PROVIDER).isEmpty());
        Assert.assertTrue(extensionLoader2.getActivateExtensions(ActivationGroup.CONSUMER).isEmpty());
    }

    @Test
    public void testGetActivateExtensionsUseFilter() {
        // Note that the result returned by getActivateExtensions is sorted.
        Set<String> extNames = new HashSet<>(Arrays.asList("impl8", "impl7", "impl1"));
        ExtensionLoader<Ext3> extensionLoader = ExtensionLoader.getExtensionLoader(Ext3.class);
        List<Ext3> exts = extensionLoader.getExtensions(extClass -> extNames.contains(extClass.getName()));
        Assert.assertTrue(exts.get(0) instanceof Ext3Impl1);
        Assert.assertTrue(exts.get(1) instanceof Ext3Impl7);
        Assert.assertTrue(exts.get(2) instanceof Ext3Impl8);
        List<Class<? extends Ext3>> extClasses =
                Arrays.asList(Ext3Impl1.class, Ext3Impl7.class, Ext3Impl8.class);
        assertExtensionClassesEqual(exts, extClasses);
    }

    @Test
    public void testMultiThread() {
        final int workerNum = 4;
        final CyclicBarrier barrier = new CyclicBarrier(workerNum);
        final CountDownLatch latch = new CountDownLatch(workerNum);
        final ExtensionLoader[] loaders = new ExtensionLoader[workerNum];
        final Ext4[] exts = new Ext4[workerNum];
        for (int i = 0; i < workerNum; i++) {
            final int threadNo = i;
            new Thread(() -> {
                try {
                    // Try to make all threads start at the same time.
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    ExtensionLoader<Ext4> extensionLoader = ExtensionLoader
                            .getExtensionLoader(Ext4.class);
                    loaders[threadNo] = extensionLoader;
                    exts[threadNo] = extensionLoader.getExtension("impl1");
                } finally {
                    latch.countDown();
                }
            }, "test-thread-" + workerNum).start();
        }
        try {
            latch.await();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        ExtensionLoader<Ext4> extensionLoader = ExtensionLoader.getExtensionLoader(Ext4.class);
        Ext4 ext = extensionLoader.getExtension("impl1");
        ext.echo();
        for (int i = 0; i < workerNum; i++) {
            ExtensionLoader loader = loaders[i];
            Assert.assertSame(extensionLoader, loader);
            Assert.assertSame(ext, exts[i]);
        }
    }
}