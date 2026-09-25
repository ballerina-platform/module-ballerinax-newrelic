// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

// startTracerProvider() hands the configured values to the native New Relic
// tracer provider, which only builds the exporter (nothing is sent until a
// span is exported), so these are safe to run without network access.

@test:Config {}
function testStartTracerProviderWithSingleApiKey() {
    startTracerProvider("test-api-key");
}

@test:Config {}
function testStartTracerProviderWithMultipleApiKeys() {
    startTracerProvider(["test-api-key-1", "test-api-key-2"]);
}

@test:Config {}
function testStartTracerProviderWithEmptyApiKeyArray() {
    // The native side reports "error: empty API key array provided", which is logged.
    startTracerProvider([]);
}

@test:Config {}
function testExternStartPublishingTracesSingleKey() {
    string[] output = externStartPublishingTraces("test-api-key", "us", "const", 0, 1000, 10000);
    test:assertEquals(output, ["ballerina: started publishing traces to New Relic on https://otlp.nr-data.net:4317"]);
}

@test:Config {}
function testExternStartPublishingTracesEuRegion() {
    string[] output = externStartPublishingTraces("test-api-key", "eu", "const", 0, 1000, 10000);
    test:assertEquals(output,
            ["ballerina: started publishing traces to New Relic on https://otlp.eu01.nr-data.net:4317"]);
}

@test:Config {}
function testExternStartPublishingTracesAllSamplerTypes() {
    foreach string samplerType in ["const", "probabilistic", "ratelimiting"] {
        string[] output = externStartPublishingTraces(["test-api-key"], "us", samplerType, 1, 1000, 10000);
        test:assertEquals(output.length(), 1, "sampler type: " + samplerType);
        test:assertTrue(output[0].startsWith("ballerina: started publishing traces"), "sampler type: " + samplerType);
    }
}

@test:Config {}
function testExternStartPublishingTracesEmptyKeyArray() {
    string[] output = externStartPublishingTraces([], "us", "const", 0, 1000, 10000);
    test:assertEquals(output, ["error: empty API key array provided"]);
}

@test:Config {}
function testDefaultConfigurables() {
    test:assertEquals(DEFAULT_SAMPLER_TYPE, "const");
    test:assertEquals(NEW_RELIC_API_KEY_ENV, "BALLERINA_NEW_RELIC_API_KEY");
    test:assertEquals(PROVIDER_NAME, "newrelic");
    test:assertEquals(REPORTER_NAME, "newrelic");
    test:assertEquals(region, "us");
    test:assertEquals(tracingReporterFlushInterval, 15000);
    test:assertEquals(metricReporterFlushInterval, 15000);
    test:assertFalse(isTraceLoggingEnabled);
    test:assertFalse(isPayloadLoggingEnabled);
}
