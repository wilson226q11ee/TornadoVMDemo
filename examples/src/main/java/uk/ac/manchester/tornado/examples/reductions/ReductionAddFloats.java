/*
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
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

package uk.ac.manchester.tornado.examples.reductions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.TornadoDriver;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.annotations.Reduce;
import uk.ac.manchester.tornado.api.enums.TornadoDeviceType;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntime;;

public class ReductionAddFloats {

    private static final int MAX_ITERATIONS = 101;

    public static void reductionAddFloats(float[] input, @Reduce float[] result) {
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] += input[i];
        }
    }

    public double computeMedian(ArrayList<Long> input) {
        Collections.sort(input);
        double middle = input.size() / 2;
        if (input.size() % 2 == 1) {
            middle = (input.get(input.size() / 2) + input.get(input.size() / 2 - 1)) / 2;
        }
        return middle;
    }

    public TornadoDeviceType getDefaultDeviceType() {
        TornadoDriver driver = TornadoRuntime.getTornadoRuntime().getDriver(0);
        return driver.getTypeDefaultDevice();
    }

    public void run(int size) {
        float[] input = new float[size];

        int numGroups = 1;
        if (size > 256) {
            numGroups = size / 256;
        }
        float[] result = null;

        TornadoDeviceType deviceType = getDefaultDeviceType();
        switch (deviceType) {
            case CPU:
                result = new float[Runtime.getRuntime().availableProcessors()];
                numGroups = Runtime.getRuntime().availableProcessors();
                break;
            case DEFAULT:
                break;
            case GPU:
                result = new float[numGroups];
                break;
            default:
                break;
        }

        Random r = new Random();
        IntStream.range(0, size).sequential().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", ReductionAddFloats::reductionAddFloats, input, result)
            .streamOut(result);
        //@formatter:on

        ArrayList<Long> timers = new ArrayList<>();
        for (int i = 0; i < MAX_ITERATIONS; i++) {

            long start = System.nanoTime();
            task.execute();
            long end = System.nanoTime();

            for (int j = 1; j < result.length; j++) {
                result[0] += result[j];
            }

            timers.add((end - start));
        }

        System.out.println("Median TotalTime: " + computeMedian(timers));
    }

    public static void main(String[] args) {
        int inputSize = 8192;
        if (args.length > 0) {
            inputSize = Integer.parseInt(args[0]);
        }
        System.out.println("Size = " + inputSize);
        new ReductionAddFloats().run(inputSize);
    }
}
