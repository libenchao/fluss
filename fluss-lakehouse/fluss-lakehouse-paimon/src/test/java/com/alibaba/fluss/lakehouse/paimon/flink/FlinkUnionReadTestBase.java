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

package com.alibaba.fluss.lakehouse.paimon.flink;

import com.alibaba.fluss.config.ConfigOptions;
import com.alibaba.fluss.lakehouse.paimon.testutils.PaimonSyncTestBase;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.alibaba.fluss.connector.flink.FlinkConnectorOptions.BOOTSTRAP_SERVERS;

class FlinkUnionReadTestBase extends PaimonSyncTestBase {

    protected static final int DEFAULT_BUCKET_NUM = 3;
    StreamTableEnvironment batchTEnv;

    @BeforeAll
    protected static void beforeAll() {
        PaimonSyncTestBase.beforeAll();
    }

    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        String bootstrapServers = String.join(",", clientConf.get(ConfigOptions.BOOTSTRAP_SERVERS));
        // create table environment
        batchTEnv = StreamTableEnvironment.create(execEnv, EnvironmentSettings.inBatchMode());
        // crate catalog using sql
        batchTEnv.executeSql(
                String.format(
                        "create catalog %s with ('type' = 'fluss', '%s' = '%s')",
                        CATALOG_NAME, BOOTSTRAP_SERVERS.key(), bootstrapServers));
        batchTEnv.executeSql("use catalog " + CATALOG_NAME);
        batchTEnv.executeSql("use " + DEFAULT_DB);
    }
}