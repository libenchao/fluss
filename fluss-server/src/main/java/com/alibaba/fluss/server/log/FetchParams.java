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

package com.alibaba.fluss.server.log;

import com.alibaba.fluss.annotation.VisibleForTesting;
import com.alibaba.fluss.record.FileLogProjection;
import com.alibaba.fluss.types.RowType;

import javax.annotation.Nullable;

import java.util.Objects;

/** Fetch data params. */
public final class FetchParams {
    /** Value -2L means we will fetch from log start offset. */
    public static final long FETCH_FROM_EARLIEST_OFFSET = -2L;

    private final int replicaId;
    // Currently, FetchOnlyLeader can be set to false only for test,
    // which indicate that the client can read log data from follower.
    private final boolean fetchOnlyLeader;
    private final FetchIsolation fetchIsolation;

    // need to read at least one message
    private boolean minOneMessage;
    // max bytes to fetch
    private int maxFetchBytes;
    // the offset to start fetching from
    private long fetchOffset;
    // whether column projection is enabled
    private boolean projectionEnabled = false;
    // the lazily initialized projection util to read and project file logs
    @Nullable private FileLogProjection fileLogProjection;

    // TODO: add more params like epoch, minBytes etc.

    public FetchParams(int replicaId, int maxFetchBytes) {
        this(replicaId, true, maxFetchBytes);
    }

    @VisibleForTesting
    public FetchParams(int replicaId, boolean fetchOnlyLeader, int maxFetchBytes) {
        this.replicaId = replicaId;
        this.fetchOnlyLeader = fetchOnlyLeader;
        this.maxFetchBytes = maxFetchBytes;
        this.fetchIsolation = FetchIsolation.of(replicaId >= 0);
        this.minOneMessage = true;
        this.fetchOffset = -1;
    }

    public void setCurrentFetch(
            long tableId,
            long fetchOffset,
            int maxFetchBytes,
            RowType schema,
            @Nullable int[] projectedFields) {
        this.fetchOffset = fetchOffset;
        this.maxFetchBytes = maxFetchBytes;
        if (projectedFields != null) {
            projectionEnabled = true;
            if (fileLogProjection == null) {
                fileLogProjection = new FileLogProjection();
            }
            fileLogProjection.setCurrentProjection(tableId, schema, projectedFields);
        } else {
            projectionEnabled = false;
        }
    }

    /**
     * Returns the projection util to read and project file logs. Returns null if there is no
     * projection registered for the current fetch.
     */
    @Nullable
    public FileLogProjection projection() {
        if (projectionEnabled) {
            return fileLogProjection;
        } else {
            return null;
        }
    }

    /**
     * Marks that at least one message has been read. This turns off the {@link #minOneMessage}
     * flag.
     */
    public void markReadOneMessage() {
        this.minOneMessage = false;
    }

    /**
     * Returns true if at least one message should be read. This is used to determine if the fetcher
     * should read at least one message even if the message size exceeds the {@link #maxFetchBytes}.
     */
    public boolean minOneMessage() {
        return minOneMessage;
    }

    public FetchIsolation isolation() {
        return fetchIsolation;
    }

    public int maxFetchBytes() {
        return maxFetchBytes;
    }

    public int replicaId() {
        return replicaId;
    }

    public boolean isFromFollower() {
        return replicaId >= 0;
    }

    public boolean fetchOnlyLeader() {
        return isFromFollower() || fetchOnlyLeader;
    }

    public long fetchOffset() {
        return fetchOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FetchParams that = (FetchParams) o;
        return replicaId == that.replicaId && maxFetchBytes == that.maxFetchBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicaId, maxFetchBytes);
    }

    @Override
    public String toString() {
        return "FetchParams("
                + ", replicaId="
                + replicaId
                + ", maxFetchBytes="
                + maxFetchBytes
                + ')';
    }
}