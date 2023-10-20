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

package com.tencent.trpc.registry.common;

import com.tencent.trpc.core.utils.ConcurrentHashSet;
import com.tencent.trpc.registry.center.NotifyListener;


/**
 * Registry listener. Associated with subscribed services, multiple listeners can be bound to each subscribed service.
 */
public class RegistryCenterListenerSet {

    /**
     * The list of associated listeners after subscribing to a service.
     */
    private final ConcurrentHashSet<NotifyListener> notifyListeners = new ConcurrentHashSet<>();

    /**
     * Get the list of associated listeners.
     *
     * @return The list of associated listeners.
     */
    public ConcurrentHashSet<NotifyListener> getNotifyListeners() {
        return notifyListeners;
    }

    /**
     * Add a listener.
     *
     * @param notifyListener The listener to be added.
     */
    public void addNotifyListener(NotifyListener notifyListener) {
        this.notifyListeners.add(notifyListener);
    }

    /**
     * Remove a listener.
     *
     * @param notifyListener The listener to be removed.
     */
    public void removeNotifyListener(NotifyListener notifyListener) {
        this.notifyListeners.remove(notifyListener);
    }

    /**
     * Check if the listener list is empty. If it is empty, it means there are no associated listeners.
     *
     * @return true if the list is empty.
     */
    public boolean isEmpty() {
        return notifyListeners.isEmpty();
    }

}
