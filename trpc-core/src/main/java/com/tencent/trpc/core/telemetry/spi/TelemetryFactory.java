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

package com.tencent.trpc.core.telemetry.spi;

import com.tencent.trpc.core.extension.Extensible;

/**
 * Telemetry plugin SPI, an abstract interface without methods, mainly used for initializing plugin content.
 *
 * <p>Used for link tracking, telemetry specification compliant plugins can be integrated, default implementation is
 * OpenTelemetry.</p>
 */
@Extensible("opentelemetry")
public interface TelemetryFactory {

}
