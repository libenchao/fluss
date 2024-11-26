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

package com.alibaba.fluss.connector.flink.source.lookup;

import com.alibaba.fluss.client.Connection;
import com.alibaba.fluss.client.ConnectionFactory;
import com.alibaba.fluss.client.table.Table;
import com.alibaba.fluss.config.Configuration;
import com.alibaba.fluss.connector.flink.utils.FlinkConversions;
import com.alibaba.fluss.connector.flink.utils.FlinkRowToFlussRowConverter;
import com.alibaba.fluss.connector.flink.utils.FlussRowToFlinkRowConverter;
import com.alibaba.fluss.metadata.TablePath;
import com.alibaba.fluss.row.InternalRow;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.utils.ProjectedRowData;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.LookupFunction;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/** A flink lookup function for fluss. */
public class FlinkLookupFunction extends LookupFunction {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkLookupFunction.class);
    private static final long serialVersionUID = 1L;

    private final Configuration flussConfig;
    private final TablePath tablePath;
    private final int maxRetryTimes;
    private final RowType flinkRowType;
    private final int[] pkIndexes;
    private final LookupNormalizer lookupNormalizer;
    @Nullable private final int[] projection;

    private transient FlinkRowToFlussRowConverter flinkRowToFlussRowConverter;
    private transient FlussRowToFlinkRowConverter flussRowToFlinkRowConverter;
    private transient Connection connection;
    private transient Table table;
    private transient ProjectedRowData projectedRowData;

    public FlinkLookupFunction(
            Configuration flussConfig,
            TablePath tablePath,
            RowType flinkRowType,
            int[] pkIndexes,
            int maxRetryTimes,
            LookupNormalizer lookupNormalizer,
            @Nullable int[] projection) {
        this.flussConfig = flussConfig;
        this.tablePath = tablePath;
        this.maxRetryTimes = maxRetryTimes;
        this.flinkRowType = flinkRowType;
        this.pkIndexes = pkIndexes;
        this.lookupNormalizer = lookupNormalizer;
        this.projection = projection;
    }

    private RowType toPkRowType(RowType rowType, int[] pkIndex) {
        LogicalType[] types = new LogicalType[pkIndex.length];
        String[] names = new String[pkIndex.length];
        for (int i = 0; i < pkIndex.length; i++) {
            types[i] = rowType.getTypeAt(pkIndex[i]);
            names[i] = rowType.getFieldNames().get(pkIndex[i]);
        }
        return RowType.of(rowType.isNullable(), types, names);
    }

    @Override
    public void open(FunctionContext context) {
        LOG.info("start open ...");
        connection = ConnectionFactory.createConnection(flussConfig);
        table = connection.getTable(tablePath);
        if (projection != null) {
            projectedRowData = ProjectedRowData.from(projection);
        }
        // TODO: convert to Fluss GenericRow to avoid unnecessary deserialization
        flinkRowToFlussRowConverter =
                FlinkRowToFlussRowConverter.create(
                        toPkRowType(flinkRowType, pkIndexes), table.getDescriptor().getKvFormat());
        flussRowToFlinkRowConverter =
                new FlussRowToFlinkRowConverter(FlinkConversions.toFlussRowType(flinkRowType));
        LOG.info("end open.");
    }

    /**
     * The invoke entry point of lookup function.
     *
     * @param keyRow - A {@link RowData} that wraps lookup keys. Currently only support single
     *     rowkey.
     */
    @Override
    public Collection<RowData> lookup(RowData keyRow) {
        RowData normalizedKeyRow = lookupNormalizer.normalizeLookupKey(keyRow);
        LookupNormalizer.RemainingFilter remainingFilter =
                lookupNormalizer.createRemainingFilter(keyRow);
        // to lookup a key, we will need to do two data conversion,
        // first is converting from flink row to fluss row,
        // second is extracting key from the fluss row when calling method table.get(flussKeyRow)
        // todo: may be reduce to one data conversion when it's a bottle neck
        InternalRow flussKeyRow = flinkRowToFlussRowConverter.toInternalRow(normalizedKeyRow);
        for (int retry = 0; retry <= maxRetryTimes; retry++) {
            try {
                InternalRow row = table.lookup(flussKeyRow).get().getRow();
                if (row != null) {
                    // TODO: we can project fluss row first, to avoid deserialize unnecessary fields
                    RowData flinkRow = flussRowToFlinkRowConverter.toFlinkRowData(row);
                    if (remainingFilter == null || remainingFilter.isMatch(flinkRow)) {
                        return Collections.singletonList(maybeProject(flinkRow));
                    } else {
                        return Collections.emptyList();
                    }
                }
            } catch (Exception e) {
                LOG.error(String.format("Fluss lookup error, retry times = %d", retry), e);
                if (retry >= maxRetryTimes) {
                    String exceptionMsg =
                            String.format(
                                    "Execution of Fluss lookup failed, retry times = %d.", retry);
                    throw new RuntimeException(exceptionMsg, e);
                }

                try {
                    Thread.sleep(1000L * retry);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(interruptedException);
                }
            }
        }
        return Collections.emptyList();
    }

    private RowData maybeProject(RowData row) {
        if (projectedRowData == null) {
            return row;
        }
        return projectedRowData.replaceRow(row);
    }

    @Override
    public void close() throws Exception {
        LOG.info("start close ...");
        if (table != null) {
            table.close();
        }
        if (connection != null) {
            connection.close();
        }
        LOG.info("end close.");
    }
}