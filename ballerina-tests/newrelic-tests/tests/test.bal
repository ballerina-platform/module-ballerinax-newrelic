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

import ballerina/http;
import ballerina/observe;
import ballerina/test;

// The New Relic extension is loaded through the packaged bala, so a failure in
// its module init (missing/conflicting native jars, bad configuration) makes
// the whole test package fail to start.

http:Client testClient = check new ("http://localhost:9091/test");

@test:Config {}
function testNewRelicIsSelectedAsTracingProvider() {
    test:assertTrue(observe:isTracingEnabled());
    test:assertEquals(observe:getTracingProvider(), "newrelic");
}

@test:Config {}
function testNewRelicIsSelectedAsMetricsReporter() {
    test:assertTrue(observe:isMetricsEnabled());
    test:assertEquals(observe:getMetricsReporter(), "newrelic");
}

@test:Config {}
function testObservedServiceRespondsWithExtensionLoaded() returns error? {
    string response = check testClient->get("/sum");
    test:assertEquals(response, "Sum: 53");
}

@test:Config {dependsOn: [testObservedServiceRespondsWithExtensionLoaded]}
function testRequestsAreRecordedAsMetrics() {
    observe:Metric[]? metrics = observe:getAllMetrics();
    test:assertTrue(metrics is observe:Metric[] && metrics.length() > 0,
            "expected the observed request to be recorded in the metrics registry");
}
