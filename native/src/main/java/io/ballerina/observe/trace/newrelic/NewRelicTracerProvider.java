/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.trace.newrelic;

import io.ballerina.observe.trace.newrelic.sampler.RateLimitingSampler;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.util.concurrent.TimeUnit;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

/**
 * This is the New Relic tracing extension class for {@link TracerProvider}.
 */
public class NewRelicTracerProvider implements TracerProvider {
    private static final String TRACER_NAME = "newrelic";
    private static final String CONST_SAMPLER_TYPE = "const";
    private static final String PROBABILISTIC_SAMPLER_TYPE = "probabilistic";
    private static final String TRACE_REPORTER_ENDPOINT = "https://otlp.nr-data.net:4317";
    private static final String TRACE_API_KEY_HEADER = "Api-Key";

    static SdkTracerProviderBuilder tracerProviderBuilder;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public void init() {
        // Do Nothing
    }

    public static BArray startPublishingTraces(BString apiKey, BString samplerType, BDecimal samplerParam,
                                                int reporterFlushInterval, int reporterBufferSize) {
        BArray output = ValueCreator.createArrayValue(TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING));

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(TRACE_REPORTER_ENDPOINT)
                .addHeader(TRACE_API_KEY_HEADER, apiKey.getValue())
                .build();

        tracerProviderBuilder = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor
                        .builder(exporter)
                        .setMaxExportBatchSize(reporterBufferSize)
                        .setExporterTimeout(reporterFlushInterval, TimeUnit.MILLISECONDS)
                        .build());

        tracerProviderBuilder.setSampler(selectSampler(samplerType, samplerParam));
        output.append(StringUtils.fromString("ballerina: started publishing traces to New Relic on " +
                TRACE_REPORTER_ENDPOINT));

        return output;
    }

    private static Sampler selectSampler(BString samplerType, BDecimal samplerParam) {
        switch (samplerType.getValue()) {
            default:
            case CONST_SAMPLER_TYPE:
                if (samplerParam.value().intValue() == 0) {
                    return Sampler.alwaysOff();
                } else {
                    return Sampler.alwaysOn();
                }
            case PROBABILISTIC_SAMPLER_TYPE:
                return Sampler.traceIdRatioBased(samplerParam.value().doubleValue());
            case RateLimitingSampler.TYPE:
                return new RateLimitingSampler(samplerParam.value().intValue());
        }
    }

    @Override
    public Tracer getTracer(String serviceName) {

        return tracerProviderBuilder.setResource(
                        Resource.create(Attributes.of(SERVICE_NAME, serviceName)))
                .build().get("jaeger");
    }

    @Override
    public ContextPropagators getPropagators() {
        return ContextPropagators.create(B3Propagator.injectingSingleHeader());
    }
}
