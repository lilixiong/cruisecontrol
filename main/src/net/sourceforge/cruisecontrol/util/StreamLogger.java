/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import java.io.InputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Logs the content of a Stream line by line.
 */
public final class StreamLogger implements StreamConsumer {
    private final Logger logger;
    private final Level level;

    private StreamLogger(Logger log, Level level) {
        this.logger = log;
        this.level = level;
    }

    public static StreamConsumer getDebugLogger(Logger log) {
        return new StreamLogger(log, Level.DEBUG);
    }

    public static StreamConsumer getInfoLogger(Logger log) {
        return new StreamLogger(log, Level.INFO);
    }

    public static StreamPumper getInfoPumper(Logger log, InputStream info) {
        return new StreamPumper(info, new StreamLogger(log, Level.INFO));
    }

    public static StreamPumper getInfoPumper(Logger log, Process process) {
        return getInfoPumper(log, process.getInputStream());
    }

    public static StreamConsumer getWarnLogger(Logger log) {
        return new StreamLogger(log, Level.WARN);
    }

    public static StreamPumper getWarnPumper(Logger log, InputStream warn) {
        return new StreamPumper(warn, new StreamLogger(log, Level.WARN));
    }

    public static StreamPumper getWarnPumper(Logger log, Process process) {
        return getWarnPumper(log, process.getErrorStream());
    }

    /** {@inheritDoc} */
    public void consumeLine(String line) {
        logger.log(level, line);
    }
}
