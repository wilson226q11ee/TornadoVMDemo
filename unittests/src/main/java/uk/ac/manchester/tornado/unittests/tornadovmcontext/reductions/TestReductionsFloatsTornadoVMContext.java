/*
 * Copyright (c) 2021, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.unittests.tornadovmcontext.reductions;

import org.junit.Test;
import uk.ac.manchester.tornado.api.GridTask;
import uk.ac.manchester.tornado.api.TornadoVMContext;
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid1D;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * The unit-tests in this class implement some Reduction operations (add, max,
 * min) for {@link Float} type. These unit-tests check the functional operation
 * of some {@link TornadoVMContext} features, such as global thread identifiers,
 * local thread identifiers, the local group size of the associated WorkerGrid,
 * barriers and allocation of local memory.
 */
public class TestReductionsFloatsTornadoVMContext extends TornadoTestBase {

    public static float computeAddSequential(float[] input) {
        float acc = 0;
        for (float v : input) {
            acc += v;
        }
        return acc;
    }

    public static void floatReductionAddGlobalMemory(TornadoVMContext context, float[] a, float[] b) {
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID
        int id = localGroupSize * groupID + localIdx;

        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                a[id] += a[id + stride];
            }
        }
        context.globalBarrier();
        if (localIdx == 0) {
            b[groupID] = a[id];
        }
    }

    @Test
    public void testFloatReductionsAddGlobalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeAddSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionAddGlobalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum += v;
        }

        assertEquals(sequential, finalSum, 0);
    }

    public static void floatReductionAddLocalMemory(TornadoVMContext context, float[] a, float[] b) {
        int globalIdx = context.threadIdx;
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID

        float[] localA = context.allocateFloatLocalArray(256);
        localA[localIdx] = a[globalIdx];
        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                localA[localIdx] += localA[localIdx + stride];
            }
        }
        if (localIdx == 0) {
            b[groupID] = localA[0];
        }
    }

    @Test
    public void testFloatReductionsAddLocalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeAddSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionAddLocalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum += v;
        }

        assertEquals(sequential, finalSum, 0);
    }

    public static float computeMaxSequential(float[] input) {
        float acc = 0;
        for (float v : input) {
            acc = Math.max(acc, v);
        }
        return acc;
    }

    private static void floatReductionMaxGlobalMemory(TornadoVMContext context, float[] a, float[] b) {
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID
        int id = localGroupSize * groupID + localIdx;

        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                a[id] = Math.max(a[id], a[id + stride]);
            }
        }
        context.globalBarrier();
        if (localIdx == 0) {
            b[groupID] = a[id];
        }
    }

    @Test
    public void testFloatReductionsMaxGlobalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeMaxSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionMaxGlobalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum = Math.max(finalSum, v);
        }

        assertEquals(sequential, finalSum, 0);
    }

    public static void floatReductionMaxLocalMemory(TornadoVMContext context, float[] a, float[] b) {
        int globalIdx = context.threadIdx;
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID

        float[] localA = context.allocateFloatLocalArray(256);
        localA[localIdx] = a[globalIdx];
        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                localA[localIdx] = Math.max(localA[localIdx], localA[localIdx + stride]);
            }
        }
        if (localIdx == 0) {
            b[groupID] = localA[0];
        }
    }

    @Test
    public void testFloatReductionsMaxLocalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeMaxSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionMaxLocalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum = Math.max(finalSum, v);
        }

        assertEquals(sequential, finalSum, 0);
    }

    public static float computeMinSequential(float[] input) {
        float acc = 0;
        for (float v : input) {
            acc = Math.min(acc, v);
        }
        return acc;
    }

    private static void floatReductionMinGlobalMemory(TornadoVMContext context, float[] a, float[] b) {
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID
        int id = localGroupSize * groupID + localIdx;

        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                a[id] = Math.min(a[id], a[id + stride]);
            }
        }
        context.globalBarrier();
        if (localIdx == 0) {
            b[groupID] = a[id];
        }
    }

    @Test
    public void testFloatReductionsMinGlobalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeMinSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionMinGlobalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum = Math.min(finalSum, v);
        }

        assertEquals(sequential, finalSum, 0);
    }

    public static void floatReductionMinLocalMemory(TornadoVMContext context, float[] a, float[] b) {
        int globalIdx = context.threadIdx;
        int localIdx = context.localIdx;
        int localGroupSize = context.getLocalGroupSize(0);
        int groupID = context.groupIdx; // Expose Group ID

        float[] localA = context.allocateFloatLocalArray(256);
        localA[localIdx] = a[globalIdx];
        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                localA[localIdx] = Math.min(localA[localIdx], localA[localIdx + stride]);
            }
        }
        if (localIdx == 0) {
            b[groupID] = localA[0];
        }
    }

    @Test
    public void testFloatReductionsMinLocalMemory() {
        final int size = 1024;
        final int localSize = 256;
        float[] input = new float[size];
        float[] reduce = new float[size / localSize];
        IntStream.range(0, input.length).sequential().forEach(i -> input[i] = i);
        float sequential = computeMinSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridTask gridTask = new GridTask("s0.t0", worker);
        TornadoVMContext context = new TornadoVMContext(worker);

        TaskSchedule s0 = new TaskSchedule("s0") //
                .streamIn(input, localSize) //
                .task("t0", TestReductionsFloatsTornadoVMContext::floatReductionMinLocalMemory, context, input, reduce) //
                .streamOut(reduce);
        // Change the Grid
        worker.setGlobalWork(size, 1, 1);
        worker.setLocalWork(localSize, 1, 1);
        s0.execute(gridTask);

        // Final SUM
        float finalSum = 0;
        for (float v : reduce) {
            finalSum = Math.min(finalSum, v);
        }

        assertEquals(sequential, finalSum, 0);
    }
}