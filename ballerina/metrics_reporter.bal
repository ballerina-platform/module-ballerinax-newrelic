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

import ballerina/jballerina.java;
import ballerina/log;

configurable int metricReporterFlushInterval = 15000;
configurable int metricReporterClientTimeout = 10000;
configurable map<string> additionalAttributes = {};

isolated function startMetricsReporter(string|string[] apiKey) {
    string[] output = externSendMetrics(apiKey, metricReporterFlushInterval, metricReporterClientTimeout, 
        isTraceLoggingEnabled, isPayloadLoggingEnabled, additionalAttributes);
    
    foreach string outputLine in output {
        if (outputLine.startsWith("error:")) {
            log:printError(outputLine);
        } else {
            log:printInfo(outputLine);
        }
    }
}

isolated function externSendMetrics(string|string[] apiKey, int metricReporterFlushInterval, int metricReporterClientTimeout, 
    boolean isTraceLoggingEnabled, boolean isPayloadLoggingEnabled, map<string> additionalAttributes) returns string[] = @java:Method {
    'class: "io.ballerina.observe.metrics.newrelic.NewRelicMetricsReporter",
    name: "sendMetrics"
} external;
