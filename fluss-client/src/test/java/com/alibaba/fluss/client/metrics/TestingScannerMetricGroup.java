/*
 * Copyright (c) 2024 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.fluss.client.metrics;

import com.alibaba.fluss.metadata.TablePath;
import com.alibaba.fluss.rpc.metrics.TestingClientMetricGroup;

/** The testing metric group for scanner. */
public class TestingScannerMetricGroup extends ScannerMetricGroup {

    private static final TablePath TABLE_PATH = TablePath.of("db", "table");

    public TestingScannerMetricGroup() {
        super(TestingClientMetricGroup.newInstance(), TABLE_PATH);
    }

    public static TestingScannerMetricGroup newInstance() {
        return new TestingScannerMetricGroup();
    }
}