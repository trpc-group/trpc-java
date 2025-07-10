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

package com.tencent.trpc.spring.boot.starters.env;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Origin trackable composite {@link PropertySource}
 *
 * @see CompositePropertySource Composite source
 */
public class OriginTrackedCompositePropertySource extends CompositePropertySource implements OriginLookup<String> {

    private final boolean immutable;

    public OriginTrackedCompositePropertySource(String name) {
        this(name, false);
    }

    public OriginTrackedCompositePropertySource(String name, boolean immutable) {
        super(name);
        this.immutable = immutable;
    }

    @Override
    public Origin getOrigin(String key) {
        for (PropertySource<?> propertySource : getPropertySources()) {
            Origin candidate = OriginLookup.getOrigin(propertySource, key);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }
}