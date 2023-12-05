// Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/jballerina.java;

const DEFAULT_SAMPLER_TYPE = "const";

configurable string tracingSamplerType = "const";
configurable decimal tracingSamplerParam = 1;
configurable int tracingReporterFlushInterval = 15000;
configurable int tracingReporterBufferSize = 10000;

function startTracerProvider(string apiKey) {
    string selectedSamplerType;

    if tracingSamplerType != "const" && tracingSamplerType != "ratelimiting" && tracingSamplerType != "probabilistic" {
        selectedSamplerType = DEFAULT_SAMPLER_TYPE;
        log:printError("error: invalid New Relic configuration sampler type: " + tracingSamplerType
                                            + ". using default " + DEFAULT_SAMPLER_TYPE + " sampling");
    } else {
        selectedSamplerType = tracingSamplerType;
    }

    externStartPublishingTraces(apiKey, selectedSamplerType, tracingSamplerParam,
        tracingReporterFlushInterval, tracingReporterBufferSize);
}

function externStartPublishingTraces(string apiKey, string samplerType,
        decimal samplerParam, int reporterFlushInterval, int reporterBufferSize) = @java:Method {
    'class: "io.ballerina.observe.trace.newrelic.NewRelicTracerProvider",
    name: "startPublishingTraces"
} external;
