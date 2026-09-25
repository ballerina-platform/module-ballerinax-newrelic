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

@test:Config {}
function testParseStringArraySingleKey() {
    test:assertEquals(parseStringArray("key-1"), "key-1");
}

@test:Config {}
function testParseStringArraySingleKeyIsTrimmed() {
    test:assertEquals(parseStringArray("  key-1  "), "key-1");
}

@test:Config {}
function testParseStringArrayMultipleKeys() {
    test:assertEquals(parseStringArray("key-1,key-2,key-3"), ["key-1", "key-2", "key-3"]);
}

@test:Config {}
function testParseStringArrayTrimsEachKey() {
    test:assertEquals(parseStringArray(" key-1 ,  key-2,key-3  "), ["key-1", "key-2", "key-3"]);
}

@test:Config {}
function testParseStringArrayTwoKeys() {
    string|string[] result = parseStringArray("a,b");
    test:assertTrue(result is string[]);
    test:assertEquals((<string[]>result).length(), 2);
}
