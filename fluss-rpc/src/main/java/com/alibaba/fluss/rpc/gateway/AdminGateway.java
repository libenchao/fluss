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

package com.alibaba.fluss.rpc.gateway;

import com.alibaba.fluss.rpc.messages.CreateDatabaseRequest;
import com.alibaba.fluss.rpc.messages.CreateDatabaseResponse;
import com.alibaba.fluss.rpc.messages.CreateTableRequest;
import com.alibaba.fluss.rpc.messages.CreateTableResponse;
import com.alibaba.fluss.rpc.messages.DropDatabaseRequest;
import com.alibaba.fluss.rpc.messages.DropDatabaseResponse;
import com.alibaba.fluss.rpc.messages.DropTableRequest;
import com.alibaba.fluss.rpc.messages.DropTableResponse;
import com.alibaba.fluss.rpc.protocol.ApiKeys;
import com.alibaba.fluss.rpc.protocol.RPC;

import java.util.concurrent.CompletableFuture;

/** The gateway interface between the client and the server for reading and writing metadata. */
public interface AdminGateway extends AdminReadOnlyGateway {
    /**
     * Create a database.
     *
     * @param request Create database request
     */
    @RPC(api = ApiKeys.CREATE_DATABASE)
    CompletableFuture<CreateDatabaseResponse> createDatabase(CreateDatabaseRequest request);

    /**
     * Drop a database.
     *
     * @param request Drop database request.
     */
    @RPC(api = ApiKeys.DROP_DATABASE)
    CompletableFuture<DropDatabaseResponse> dropDatabase(DropDatabaseRequest request);

    /**
     * Creates a new table.
     *
     * @param request the request to create table.
     */
    @RPC(api = ApiKeys.CREATE_TABLE)
    CompletableFuture<CreateTableResponse> createTable(CreateTableRequest request);

    /**
     * Drop a table.
     *
     * @param request Drop table request
     */
    @RPC(api = ApiKeys.DROP_TABLE)
    CompletableFuture<DropTableResponse> dropTable(DropTableRequest request);

    // todo: rename table & alter table
}