package uk.ac.manchester.tornado.examples.spirv;

import java.util.Arrays;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.common.Access;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntime;

/**
 * How to run?
 *
 * <code>
 * tornado --debug uk.ac.manchester.tornado.examples.spirv.TestPrecompiledSPIRV
 * </code>
 */
public class TestPrecompiledSPIRV {

    public static void main(String[] args) {
        final int numElements = 8;
        int[] a = new int[256];
        Arrays.fill(a, 1);

        TornadoDevice defaultDevice = TornadoRuntime.getTornadoRuntime().getDriver(0).getDevice(0);
        System.out.println("DEFAULT DEVICE: " + defaultDevice);

        String filePath = "/tmp/example.spv";

        // @formatter:off
        new TaskSchedule("s0")
                .prebuiltTask("t0",
                        "copyTest",
                        filePath,
                        new Object[] { a },
                        new Access[] { Access.WRITE },
                        defaultDevice,
                        new int[] { numElements })
                .streamOut(a)
                .execute();
        // @formatter:on

        System.out.println("a: " + Arrays.toString(a));
    }
}
