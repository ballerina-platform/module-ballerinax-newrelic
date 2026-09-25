/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test class for NewRelicTracerProvider.
 */
public class NewRelicTracerProviderTest {

    private static final String US_ENDPOINT = "https://otlp.nr-data.net:4317";
    private static final String EU_ENDPOINT = "https://otlp.eu01.nr-data.net:4317";

    private NewRelicTracerProvider tracerProvider;

    @BeforeMethod
    public void setUp() {
        tracerProvider = new NewRelicTracerProvider();
    }

    @Test
    public void testGetName() {
        assertEquals(tracerProvider.getName(), "newrelic");
    }

    @Test
    public void testInit() {
        // init() does nothing; just ensure it doesn't throw
        tracerProvider.init();
    }

    @Test
    public void testGetPropagators() {
        ContextPropagators propagators = tracerProvider.getPropagators();
        assertNotNull(propagators);
        assertTrue(propagators.getTextMapPropagator().fields().contains("traceparent"));
    }

    @Test
    public void testStartPublishingTracesWithSingleApiKey() {
        BArray output = start(StringUtils.fromString("test-key"), "us", "const", 0);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "ballerina: started publishing traces to New Relic on " + US_ENDPOINT);
    }

    @Test
    public void testStartPublishingTracesWithMultipleApiKeys() {
        BArray keys = ValueCreator.createArrayValue(new BString[]{
                StringUtils.fromString("key-1"), StringUtils.fromString("key-2")});

        BArray output = start(keys, "us", "const", 0);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "ballerina: started publishing traces to New Relic on " + US_ENDPOINT);
    }

    @Test
    public void testStartPublishingTracesReportsEuEndpoint() {
        for (String region : new String[]{"eu", "EU"}) {
            BArray output = start(StringUtils.fromString("test-key"), region, "const", 0);
            assertEquals(output.get(0).toString(),
                    "ballerina: started publishing traces to New Relic on " + EU_ENDPOINT);
        }
    }

    @Test
    public void testStartPublishingTracesWithNullRegionUsesUsEndpoint() {
        BArray output = NewRelicTracerProvider.startPublishingTraces(StringUtils.fromString("test-key"), null,
                StringUtils.fromString("const"), BDecimal.valueOf(0), 1000, 10000);

        assertEquals(output.get(0).toString(), "ballerina: started publishing traces to New Relic on " + US_ENDPOINT);
    }

    @Test
    public void testStartPublishingTracesWithEmptyApiKeyArray() {
        BArray output = start(ValueCreator.createArrayValue(new BString[0]), "us", "const", 0);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "error: empty API key array provided");
    }

    @Test
    public void testStartPublishingTracesWithInvalidApiKeyType() {
        BArray output = start(Long.valueOf(42), "us", "const", 0);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "error: invalid API key type");
    }

    @Test
    public void testGetTracerAfterStartPublishingTraces() {
        // alwaysOff sampler (const/0): no span export is ever attempted, so this
        // is safe to run without network access to New Relic.
        start(StringUtils.fromString("test-key"), "us", "const", 0);

        Tracer tracer = tracerProvider.getTracer("new-relic-tracer-provider-test");
        assertNotNull(tracer);

        Span span = tracer.spanBuilder("test-span").startSpan();
        assertTrue(!span.getSpanContext().isSampled());
        span.end();
    }

    @Test
    public void testSelectSamplerConstOn() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("const", BDecimal.valueOf(1));
        assertEquals(sampler.getDescription(), Sampler.alwaysOn().getDescription());
    }

    @Test
    public void testSelectSamplerConstOff() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("const", BDecimal.valueOf(0));
        assertEquals(sampler.getDescription(), Sampler.alwaysOff().getDescription());
    }

    @Test
    public void testSelectSamplerProbabilistic() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("probabilistic", BDecimal.valueOf(0.5));
        assertEquals(sampler.getDescription(), Sampler.traceIdRatioBased(0.5).getDescription());
    }

    @Test
    public void testSelectSamplerRateLimiting() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler(RateLimitingSampler.TYPE, BDecimal.valueOf(5));
        assertTrue(sampler instanceof RateLimitingSampler);
    }

    @Test
    public void testSelectSamplerUnknownTypeDefaultsToConst() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("unrecognized-type", BDecimal.valueOf(1));
        assertEquals(sampler.getDescription(), Sampler.alwaysOn().getDescription());
    }

    private BArray start(Object apiKey, String region, String samplerType, int samplerParam) {
        return NewRelicTracerProvider.startPublishingTraces(apiKey, StringUtils.fromString(region),
                StringUtils.fromString(samplerType), BDecimal.valueOf(samplerParam), 1000, 10000);
    }

    private Sampler invokeSelectSampler(String samplerType, BDecimal samplerParam) throws ReflectiveOperationException {
        Method method = NewRelicTracerProvider.class.getDeclaredMethod("selectSampler", BString.class,
                BDecimal.class);
        method.setAccessible(true);
        return (Sampler) method.invoke(null, StringUtils.fromString(samplerType), samplerParam);
    }
}
