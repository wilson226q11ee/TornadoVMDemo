/*
 * Copyright (c) 2020, APT Group, Department of Computer Science,
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
package uk.ac.manchester.tornado.benchmarks.euler;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.benchmarks.ComputeKernels;

public class JMHEuler {

    @State(Scope.Thread)
    public static class BenchmarkSetup {

        private int size = Integer.parseInt(System.getProperty("x", "128"));
        long[] input;
        long[] outputA;
        long[] outputB;
        long[] outputC;
        long[] outputD;
        long[] outputE;
        private TaskGraph taskGraph;

        private long[] init(int size) {
            long[] input = new long[size];
            for (int i = 0; i < size; i++) {
                input[i] = (long) i * i * i * i * i;
            }
            return input;
        }

        @Setup(Level.Trial)
        public void doSetup() {
            input = init(size);
            outputA = new long[size];
            outputB = new long[size];
            outputC = new long[size];
            outputD = new long[size];
            outputE = new long[size];
            taskGraph = new TaskGraph("s0") //
                    .transferToDevice(DataTransferMode.EVERY_EXECUTION, input) //
                    .task("s0", ComputeKernels::euler, size, input, outputA, outputB, outputC, outputD, outputE) //
                    .transferToHost(outputA, outputB, outputC, outputD, outputE);
            taskGraph.warmup();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 60, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(1)
    public void eulerJava(BenchmarkSetup state) {
        ComputeKernels.euler(state.size, state.input, state.outputA, state.outputB, state.outputC, state.outputD, state.outputE);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(1)
    public void eulerTornado(BenchmarkSetup state, Blackhole blackhole) {
        TaskGraph t = state.taskGraph;
        t.execute();
        blackhole.consume(t);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder() //
                .include(JMHEuler.class.getName() + ".*") //
                .mode(Mode.AverageTime) //
                .timeUnit(TimeUnit.NANOSECONDS) //
                .warmupTime(TimeValue.seconds(60)) //
                .warmupIterations(2) //
                .measurementTime(TimeValue.seconds(30)) //
                .measurementIterations(5) //
                .forks(1) //
                .build();
        new Runner(opt).run();
    }
}
