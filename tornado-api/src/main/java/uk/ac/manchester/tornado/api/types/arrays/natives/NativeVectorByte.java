/*
 * Copyright (c) 2013-2023, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.api.types.arrays.natives;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import uk.ac.manchester.tornado.api.types.arrays.TornadoNativeArray;

public final class NativeVectorByte extends TornadoNativeArray {
    private final int BYTE_BYTES = 1;
    private MemorySegment segment;
    private int numberOfElements;

    private long segmentByteSize;

    public NativeVectorByte(int numberOfElements) {
        this.numberOfElements = numberOfElements;
        segmentByteSize = numberOfElements * BYTE_BYTES;
        segment = Arena.ofAuto().allocate(segmentByteSize, 1);
    }

    public void set(int index, byte value) {
        segment.setAtIndex(JAVA_BYTE, index, value);
    }

    public byte get(int index) {
        return segment.getAtIndex(JAVA_BYTE, index);
    }

    public void init(byte value) {
        for (int i = 0; i < getSize(); i++) {
            segment.setAtIndex(JAVA_BYTE, i, value);
        }
    }

    @Override
    public int getSize() {
        return numberOfElements;
    }

    @Override
    public MemorySegment getSegment() {
        return segment;
    }

    @Override
    public long getNumBytesOfSegment() {
        return segmentByteSize;
    }

    @Override
    public long getNumBytesWithoutHeader() {
        return segmentByteSize;
    }

    @Override
    protected void clear() {
        init((byte) 0);
    }
}
