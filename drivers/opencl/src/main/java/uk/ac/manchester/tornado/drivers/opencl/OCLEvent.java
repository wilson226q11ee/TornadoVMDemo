/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2020, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * Copyright (c) 2013-2020, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.drivers.opencl;

import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLCommandExecutionStatus.CL_COMPLETE;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLCommandExecutionStatus.createOCLCommandExecutionStatus;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLEventInfo.CL_EVENT_COMMAND_EXECUTION_STATUS;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLProfilingInfo.CL_PROFILING_COMMAND_END;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLProfilingInfo.CL_PROFILING_COMMAND_START;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLProfilingInfo.CL_PROFILING_COMMAND_SUBMIT;
import static uk.ac.manchester.tornado.runtime.common.Tornado.ENABLE_PROFILING;

import java.nio.ByteBuffer;

import uk.ac.manchester.tornado.api.common.Event;
import uk.ac.manchester.tornado.api.enums.TornadoExecutionStatus;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLCommandExecutionStatus;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLProfilingInfo;
import uk.ac.manchester.tornado.drivers.opencl.exceptions.OCLException;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.common.TornadoLogger;

public class OCLEvent extends TornadoLogger implements Event {

    protected static final long DEFAULT_TAG = 0x12;

    // @formatter:off
    protected static final String[] EVENT_DESCRIPTIONS = {
            "kernel - serial",
            "kernel - parallel",
            "writeToDevice - byte[]",
            "writeToDevice - short[]",
            "writeToDevice - int[]",
            "writeToDevice - long[]",
            "writeToDevice - float[]",
            "writeToDevice - double[]",
            "readFromDevice - byte[]",
            "readFromDevice - short[]",
            "readFromDevice - int[]",
            "readFromDevice - long[]",
            "readFromDevice - float[]",
            "readFromDevice - double[]",
            "sync - marker",
            "sync - barrier",
            "none"
    };
    // @formatter:on

    protected static final int DESC_SERIAL_KERNEL = 0;
    protected static final int DESC_PARALLEL_KERNEL = 1;
    protected static final int DESC_WRITE_BYTE = 2;
    protected static final int DESC_WRITE_SHORT = 3;
    protected static final int DESC_WRITE_INT = 4;
    protected static final int DESC_WRITE_LONG = 5;
    protected static final int DESC_WRITE_FLOAT = 6;
    protected static final int DESC_WRITE_DOUBLE = 7;
    protected static final int DESC_READ_BYTE = 8;
    protected static final int DESC_READ_SHORT = 9;
    protected static final int DESC_READ_INT = 10;
    protected static final int DESC_READ_LONG = 11;
    protected static final int DESC_READ_FLOAT = 12;
    protected static final int DESC_READ_DOUBLE = 13;
    protected static final int DESC_SYNC_MARKER = 14;
    protected static final int DESC_SYNC_BARRIER = 15;
    protected static final int EVENT_NONE = 16;

    private static final long[] internalBuffer = new long[2];

    private OCLEventsWrapper eventsWrapper;
    private OCLCommandQueue queue;
    private int localId;
    private long oclEventID;
    private static final ByteBuffer buffer = ByteBuffer.allocate(8);
    private String name;
    private int status;

    static {
        buffer.order(OpenCL.BYTE_ORDER);
    }

    OCLEvent(final OCLEventsWrapper eventsWrapper) {
        this.eventsWrapper = eventsWrapper;
    }

    OCLEvent(final OCLEventsWrapper eventsWrapper, final OCLCommandQueue queue, final int event, final long oclEventID) {
        this.eventsWrapper = eventsWrapper;
        this.queue = queue;
        this.localId = event;
        this.oclEventID = oclEventID;
        this.name = String.format("%s: 0x%x", EVENT_DESCRIPTIONS[eventsWrapper.getDescriptor(localId)], eventsWrapper.getTag(localId));
        this.status = -1;
    }

    void setEventId(int localId, long eventId) {
        this.localId = localId;
        this.oclEventID = eventId;
    }

    native static void clGetEventInfo(long eventId, int param, byte[] buffer) throws OCLException;

    native static void clGetEventProfilingInfo(long eventId, long param, byte[] buffer) throws OCLException;

    native static void clWaitForEvents(long[] events) throws OCLException;

    native static void clReleaseEvent(long eventId) throws OCLException;

    private long readEventTime(OCLProfilingInfo eventType) {
        if (!ENABLE_PROFILING) {
            return -1;
        }
        long time = 0;
        buffer.clear();
        try {
            clGetEventProfilingInfo(oclEventID, eventType.getValue(), buffer.array());
            time = buffer.getLong();
        } catch (OCLException e) {
            error(e.getMessage());
        }
        return time;
    }

    @Override
    public void waitForEvents() {
        try {
            clWaitForEvents(new long[] { oclEventID });
        } catch (OCLException e) {
            e.printStackTrace();
        }
    }

    long getCLSubmitTime() {
        return readEventTime(CL_PROFILING_COMMAND_SUBMIT);
    }

    long getCLStartTime() {
        return readEventTime(CL_PROFILING_COMMAND_START);
    }

    long getCLEndTime() {
        return readEventTime(CL_PROFILING_COMMAND_END);
    }

    private OCLCommandExecutionStatus getCLStatus() {
        if (status == 0) {
            return CL_COMPLETE;
        }

        buffer.clear();

        try {
            clGetEventInfo(oclEventID, CL_EVENT_COMMAND_EXECUTION_STATUS.getValue(), buffer.array());
            status = buffer.getInt();
        } catch (OCLException e) {
            error(e.getMessage());
        }

        return createOCLCommandExecutionStatus(status);
    }

    @Override
    public void waitOn() {
        switch (getCLStatus()) {
            case CL_COMPLETE:
                break;
            case CL_SUBMITTED:
                queue.flush();
            case CL_QUEUED:
            case CL_RUNNING:
                waitOnPassive();
                break;
            case CL_ERROR:
            case CL_UNKNOWN:
                fatal("error on event: %s", name);
        }
    }

    private void waitOnPassive() {
        try {
            internalBuffer[0] = 1;
            internalBuffer[1] = oclEventID;
            clWaitForEvents(internalBuffer);
        } catch (OCLException e) {
            error(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("[OCLEVENT] event: name=%s, status=%s", name, getStatus());
    }

    public long getOclEventID() {
        return oclEventID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TornadoExecutionStatus getStatus() {
        return getCLStatus().toTornadoExecutionStatus();
    }

    @Override
    public long getExecutionTime() {
        return (getCLEndTime() - getCLStartTime());
    }

    @Override
    public double getExecutionTimeInSeconds() {
        return RuntimeUtilities.elapsedTimeInSeconds(getCLStartTime(), getCLEndTime());
    }

    @Override
    public double getTotalTimeInSeconds() {
        return getExecutionTimeInSeconds();
    }

    @Override
    public long getSubmitTime() {
        return getCLSubmitTime();
    }

    @Override
    public long getStartTime() {
        return getCLStartTime();
    }

    @Override
    public long getEndTime() {
        return getCLEndTime();
    }

    void release() {
        eventsWrapper.releaseEvent(localId);
        try {
            clReleaseEvent(oclEventID);
        } catch (OCLException e) {
            error(e.getMessage());
        }
    }
}
