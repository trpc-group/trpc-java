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

package com.tencent.trpc.spring.context;

import static com.tencent.trpc.spring.util.AnnotationUtils.getMergedAttributes;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link BeanPostProcessor} implementation that perform annotation-driven injections.
 */
public abstract class AnnotationInjectedBeanPostProcessor implements InstantiationAwareBeanPostProcessor,
        MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, BeanClassLoaderAware,
        EnvironmentAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationInjectedBeanPostProcessor.class);
    private static final int CACHE_SIZE = 32;
    private final Class<? extends Annotation>[] annotationTypes;
    /**
     * Cache for injected beans
     */
    private final ConcurrentMap<String, Object> injectedObjectsCache = new ConcurrentHashMap<>(CACHE_SIZE);
    /**
     * Cache for {@link InjectionMetadata}
     */
    private final ConcurrentMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(CACHE_SIZE);
    private final Object lock = new Object();
    private ConfigurableListableBeanFactory beanFactory;
    private Environment environment;
    private ClassLoader classLoader;

    /**
     * AnnotationInjectedBeanPostProcessor Constructor
     *
     * @param annotationTypes annotation types that requires processing
     */
    @SafeVarargs
    public AnnotationInjectedBeanPostProcessor(Class<? extends Annotation>... annotationTypes) {
        Assert.notEmpty(annotationTypes, "The argument of annotations' types must not empty");
        this.annotationTypes = annotationTypes;
    }

    /**
     * Implements {@link InstantiationAwareBeanPostProcessor#postProcessProperties(PropertyValues, Object, String)}
     * to perform bean injection.
     * 
     * @param pvs the property values that the factory is about to apply (never {@code null})
     * @param bean the bean instance created, but whose properties have not yet been set
     * @param beanName the name of the bean
     * @return the actual property values to apply to the given bean
     * @throws BeansException in case of errors
     */
    @Override
    public PropertyValues postProcessProperties(@NonNull PropertyValues pvs, Object bean, @NonNull String beanName)
            throws BeansException {
        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + getAnnotationTypes()[0].getSimpleName()
                    + " dependencies is failed", ex);
        }
        return pvs;
    }

    /**
     * Post-process the given merged bean definition for the specified bean.
     *
     * @param beanDefinition the merged bean definition for the bean
     * @param beanType the actual type of the managed bean instance
     * @param beanName the name of the bean
     */
    @Override
    public void postProcessMergedBeanDefinition(@NonNull RootBeanDefinition beanDefinition,
                                                @NonNull Class<?> beanType,
                                                @NonNull String beanName) {
        InjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
        metadata.checkConfigMembers(beanDefinition);
    }

    /**
     * shutdown hook for AnnotationInjectedBeanPostProcessor
     *
     * @throws Exception in case of shutdown errors
     */
    @Override
    public void destroy() throws Exception {
        for (Object object : injectedObjectsCache.values()) {
            if (logger.isInfoEnabled()) {
                logger.info(object + " was destroying!");
            }
            if (object instanceof DisposableBean) {
                ((DisposableBean) object).destroy();
            }
        }
        injectionMetadataCache.clear();
        injectedObjectsCache.clear();
        logger.info("{} was destroying!", getClass());
    }

    /**
     * Get the order value of this object.
     * <p>Higher values are interpreted as lower priority. As a consequence,
     * the object with the lowest value has the highest priority (somewhat analogous to Servlet {@code load-on-startup}
     * values).</p>
     * <p>Same order values will result in arbitrary sort positions for the
     * affected objects.</p>
     *
     * @return the order value
     * @see #HIGHEST_PRECEDENCE
     * @see #LOWEST_PRECEDENCE
     */
    @Override
    public int getOrder() {
        // Make sure higher priority than {@link AutowiredAnnotationBeanPostProcessor}
        return Ordered.LOWEST_PRECEDENCE - 3;
    }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "AnnotationInjectedBeanPostProcessor requires a ConfigurableListableBeanFactory");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    protected ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * Gets all injected beans.
     *
     * @return non-null {@link Collection}
     */
    protected Collection<Object> getInjectedObjects() {
        return this.injectedObjectsCache.values();
    }

    /**
     * Get concerned annotation classes.
     *
     * @return array of {@link Annotation} classes
     */
    protected final Class<? extends Annotation>[] getAnnotationTypes() {
        return annotationTypes;
    }

    /**
     * Get the bean to be injected with, base on the annotation on the field that requires injection.
     *
     * @param attributes {@link AnnotationAttributes} of the annotation
     * @param bean the bean whose field requires injection
     * @param beanName name of the bean whose field requires injection
     * @param injectedType the type of the injected bean
     * @param injectedElement {@link InjectedElement}
     * @return the bean to be injected with
     */
    protected Object getInjectedObject(AnnotationAttributes attributes, Object bean,
            String beanName, Class<?> injectedType, InjectedElement injectedElement) {
        String cacheKey = buildInjectedObjectCacheKey(attributes, bean, beanName, injectedType, injectedElement);
        return injectedObjectsCache.computeIfAbsent(cacheKey, s -> {
            try {
                return doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Subclass must implement this method to provide the bean to be injected with.
     * Following context objects is provided to subclass if needed:
     * <ul>
     * <li>{@link #getBeanFactory()}</li>
     * <li>{@link #getClassLoader()}</li>
     * <li>{@link #getEnvironment()}</li>
     * </ul>
     *
     * @param attributes {@link AnnotationAttributes} of the annotation
     * @param bean the bean whose field requires injection
     * @param beanName name of the bean whose field requires injection
     * @param injectedType the type of the injected bean
     * @param injectedElement {@link InjectedElement}
     * @return the bean to be injected with
     * @throws Exception If resolving an injected object is failed.
     */
    protected abstract Object doGetInjectedBean(AnnotationAttributes attributes, Object bean,
            String beanName, Class<?> injectedType, InjectedElement injectedElement) throws Exception;

    /**
     * Build a cache key for injected beans.
     *
     * @param attributes {@link AnnotationAttributes} of the annotation
     * @param bean the bean whose field requires injection
     * @param beanName name of the bean whose field requires injection
     * @param injectedType the type of the injected bean
     * @param injectedElement {@link InjectedElement}
     * @return Bean cache key
     */
    protected abstract String buildInjectedObjectCacheKey(AnnotationAttributes attributes,
            Object bean, String beanName, Class<?> injectedType, InjectedElement injectedElement);

    /**
     * Find the {@link InjectionMetadata} of the bean requires injection.
     *
     * @param beanName name of the bean that requires injection
     * @param clazz type of the bean that requires injection
     * @param pvs {@link PropertyValues} of the bean
     * @return {@link InjectionMetadata} of the bean
     */
    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (lock) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName()
                                + "] for annotation metadata: could not find class that "
                                + "it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * Build {@link InjectionMetadata} of target bean class for annotation-driven injection.
     * By finding specified annotations({@link #annotationTypes}) on bean fields.
     *
     * @param beanClass target bean class
     * @return {@link InjectionMetadata} of the bean
     */
    private InjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        List<InjectedElement> elements = new LinkedList<>();
        ReflectionUtils.doWithFields(beanClass, field ->
                Stream.of(getAnnotationTypes()).forEach(annotationType -> {
                    AnnotationAttributes attributes = getMergedAttributes(field, annotationType, getEnvironment(),
                            Boolean.TRUE);
                    if (attributes != null) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            logger.warn("@" + annotationType.getName() + " is not supported on static fields: "
                                    + field);
                            return;
                        }
                        elements.add(new AnnotatedFieldElement(field, attributes));
                    }
                })
        );
        return new InjectionMetadata(beanClass, elements);
    }

    /**
     * Extends {@link InjectedElement} and overrides {@link #inject(Object, String, PropertyValues)}
     * to inject custom object to target field.
     */
    public class AnnotatedFieldElement extends InjectedElement {

        private final Field field;

        private final AnnotationAttributes attributes;

        protected AnnotatedFieldElement(Field field, AnnotationAttributes attributes) {
            super(field, null);
            this.field = field;
            this.attributes = attributes;
        }

        @Override
        protected void inject(@NonNull Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Class<?> injectedType = field.getType();
            Object injectedObject = getInjectedObject(attributes, bean, beanName, injectedType, this);
            ReflectionUtils.makeAccessible(field);
            field.set(bean, injectedObject);
        }
    }
}
