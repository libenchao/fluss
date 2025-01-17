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

package com.alibaba.fluss.memory;

import com.alibaba.fluss.annotation.Internal;

import javax.annotation.Nullable;

/** Interface describing entities that can provide memory segments. */
@Internal
public interface MemorySegmentSource {

    /**
     * Gets the next memory segment. If no more segments are available, it returns null.
     *
     * @param waiting Flag indicating whether the method should wait until a segment is available.
     * @return The next memory segment, or null, if none is available.
     */
    @Nullable
    MemorySegment nextSegment(boolean waiting);
}
