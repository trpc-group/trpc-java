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

package com.tencent.trpc.spring.util;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedAnnotation;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.containsElement;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.trimWhitespace;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * Annotation Utilities Class
 *
 * @see org.springframework.core.annotation.AnnotationUtils
 */
public class AnnotationUtils {

    /**
     * Get the attribute value
     *
     * @param attributes {@link AnnotationAttributes the annotation attributes}
     * @param name the name of attribute
     * @param <T> the type of attribute value
     * @return the attribute value if found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(AnnotationAttributes attributes, String name) {
        return (T) attributes.get(name);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation specified {@link Annotation}
     * @param propertyResolver {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     */
    public static Map<String, Object> getAttributes(Annotation annotation,
            PropertyResolver propertyResolver,
            boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        if (annotation == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> attributes = getAnnotationAttributes(annotation);

        Map<String, Object> actualAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            // ignore default attribute value
            if (ignoreDefaultValue && nullSafeEquals(attributeValue,
                    getDefaultValue(annotation, attributeName))) {
                continue;
            }
            actualAttributes.put(attributeName, attributeValue);
        }

        return resolvePlaceholders(actualAttributes, propertyResolver, ignoreAttributeNames);
    }

    /**
     * Resolve the placeholders from the specified annotation attributes
     *
     * @param sourceAnnotationAttributes the source of annotation attributes
     * @param propertyResolver {@link PropertyResolver}
     * @param ignoreAttributeNames the attribute names to be ignored
     * @return a new resolved annotation attributes , non-null and read-only
     */
    public static Map<String, Object> resolvePlaceholders(
            Map<String, Object> sourceAnnotationAttributes,
            PropertyResolver propertyResolver,
            String... ignoreAttributeNames) {

        if (isEmpty(sourceAnnotationAttributes)) {
            return emptyMap();
        }

        Map<String, Object> resolvedAnnotationAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : sourceAnnotationAttributes.entrySet()) {

            String attributeName = entry.getKey();

            // ignore attribute name to skip
            if (containsElement(ignoreAttributeNames, attributeName)) {
                continue;
            }

            Object attributeValue = entry.getValue();

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
            }

            resolvedAnnotationAttributes.put(attributeName, attributeValue);
        }

        return unmodifiableMap(resolvedAnnotationAttributes);
    }

    private static String resolvePlaceholders(String attributeValue,
            PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

    /**
     * Get {@link AnnotationAttributes the annotation attributes} after merging and resolving the
     * placeholders
     *
     * @param annotatedElement {@link AnnotatedElement the annotated element}
     * @param annotationType the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return If the specified annotation type is not found, return <code>null</code>
     */
    public static AnnotationAttributes getMergedAttributes(AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationType, PropertyResolver propertyResolver,
            boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        Annotation annotation = getMergedAnnotation(annotatedElement, annotationType);
        return annotation == null ? null : fromMap(getAttributes(annotation, propertyResolver,
                ignoreDefaultValue, ignoreAttributeNames));
    }
}
