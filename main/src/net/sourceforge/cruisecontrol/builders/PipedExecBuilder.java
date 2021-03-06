/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;


/**
 * Piped exec builder class.
 *
 * Executes a set of command line scripts where one can be piped from another as well as one may
 * wait until another finishes. It determines whether each of the scripts was successful or not.
 * Each script is configured independently in the same way as scripts run by {@link ExecBuilder},
 * with the extension of piping and waiting facility configured through objects returned by
 * {@link #createExec()} method.
 *
 * Individual scripts in the builder are started simultaneously whenever possible.
 *
 * Configuration example for this plugin:
 * <pre>
 *         <cruisecontrol>
 *         <schedule>
 *          <pipedexec workingdir="${workingdir.default}" timeout="3000"/>
 *               <exec id="1" command="exec1" args="-a1 -a2"     timeout="10"/>
 *               <exec id="2" command="exec2" args="-a1"         pipefrom="1"/>
 *               <exec id="3" command="exec3" args="-a1 -a2 -a3" pipefrom="2" workingdir="${workingdir.special}"/>
 *               <exec id="4" command="exec3" args="-a1 -a2"     pipefrom="1" waitfor="2"/>
 *               <exec id="5" command="exec4" args="-a1  data"   pipefrom="4"/>
 *           </piped_exec>
 *           </schedule>
 *     <cruisecontrol>
 * </pre>
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
public class PipedExecBuilder extends Builder {

    /** Serialization UID */
    private static final long serialVersionUID = -6632406315466647230L;

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(PipedExecBuilder.class);

    /** Build timeout in seconds, set by {@link #setTimeout(long)}. */
    private long timeout = ScriptRunner.NO_TIMEOUT;
    /** Keep STDOUT of all the scripts gzipped? Set by {@link #setGZipStdout(boolean)} */
    private boolean gzip;
    /** Is STDOUT of all the scripts binary? Set by {@link #setBinaryStdout(boolean)} */
    private boolean binary;
    /** The working directory where the commands are to be executed, set by
     * {@link #setWorkingDir(String)}. */
    private String workingDir;
    /** The list of scripts to execute during build. Once the script is started, it is moved
     *  to the list of started scripts. */
    private final LinkedList<PipedScript> scripts = new LinkedList<PipedScript>();

    /**
     * Validate the attributes for the plugin.
     */
    @Override
    public void validate() throws CruiseControlException {
        super.validate();

        Set<PipedScript> removeIDs = new HashSet<PipedScript>(scripts.size()); /* Scripts to be removed */
        Set<String> uniqueIDs = new HashSet<String>(scripts.size()); /* To check unique IDs */
        Set<String> notInLoop = new HashSet<String>(scripts.size()); /* Loops detection */

        /*
         * Resolve repiped scripts
         */
        for (PipedScript s : scripts) {
             if (s.getRepipe() == null) {
                 continue;
             }

             /* Repipe required, check setting */
             ValidationHelper.assertIsSet(s.getID(), "ID", s.getClass());
             /* Repipe */
             for (PipedScript c : scripts) {
                  if (c != s && s.getID().equals(c.getID())) { // ID is defined, just checked
                      c.setPipeFrom(s.getRepipe());
                  }
             }
             /* Remove the "repipe" script from the sequence */
             removeIDs.add(s);
        }
        /*
         * Remove disabled scripts
         */
        for (PipedScript s : scripts) {
            if (!s.getDisable()) {
                continue;
            }
            /* Disabled - check setting */
            ValidationHelper.assertIsSet(s.getID(), "ID", s.getClass());
            /* Remove the command from the sequence */
            for (PipedScript c : scripts) {
                 if (s.getID().equals(c.getID())) { // ID is defined, just checked
                     removeIDs.add(c);
                     removeIDs.addAll(findPipedSeq(c.getID(), scripts));
                 }
            }
            /* Remove the disabled */
            removeIDs.add(s);
        }
        scripts.removeAll(removeIDs);
        removeIDs.clear();

        /*
         * Check the (remaining) scripts for basic setting
         */
        for (PipedScript s : scripts) {
            /* Pass config variables to the exec script, if it does not have set them. Must be done
               before s.validate(), since it sets the variables to a default value */
            if (s.getWorkingDir() == null) {
                s.setWorkingDir(workingDir);
            }
            if (s.getGZipStdout() == null) {
                s.setGZipStdout(gzip);
            }
            if (s.getBinaryOutput() == null) {
                s.setBinaryOutput(binary);
            }
            /* Let it validate itself */
            s.validate();

            /* Cannot be piped or wait for itself */
            ValidationHelper.assertIsSet(s.getID(), "ID", s.getClass());
            ValidationHelper.assertFalse(s.getID().equals(s.getPipeFrom()),
                    "Script " + s.getID() + " cannot pipe from itself");
            ValidationHelper.assertFalse(s.getID().equals(s.getWaitFor()),
                    "Script " + s.getID() + " cannot wait for itself");
            /* If the script is piped from for another script, the "another: must exist */
            if (s.getPipeFrom() != null) {
                ValidationHelper.assertTrue(findToStart(s.getPipeFrom(), scripts) != null,
                        "Script " + s.getID() + " is piped from non-existing script " + s.getPipeFrom());
            }
            /* If the script waits for another script, the "another: must exist */
            if (s.getWaitFor() != null) {
                ValidationHelper.assertTrue(findToStart(s.getWaitFor(), scripts) != null,
                        "Script " + s.getID() + " waits for non-existing script " + s.getWaitFor());
            }
            /* ID must be unique */
            ValidationHelper.assertFalse(uniqueIDs.contains(s.getID()), "ID " + s.getID() + " is not unique");
            uniqueIDs.add(s.getID());
        }

        /*
         * Loops detection
         */
        for (PipedScript s : scripts) {
            notInLoop = checkLoop(s, notInLoop, new HashSet<String>());
        }
    }

    /**
     * Execute the commands and return the results as XML
     */
    @Override
    public Element build(final Map<String, String> buildProperties, final Progress progressIn)
        throws CruiseControlException {

        final ThreadPool threads = new ThreadPool();
        final long startTime = System.currentTimeMillis();
        final LinkedList<PipedScript> tostart = new LinkedList<PipedScript>(scripts);
        final LinkedList<PipedScript> started = new LinkedList<PipedScript>();


        final Element buildLogElement = new Element("build");

        /* Go through the list of scripts until all were started and finished (tostart contains
         * those not started yet, started those running or not finished yet) */
        while (tostart.size() > 0 || started.size() > 0) {
            ListIterator<PipedScript> iter = tostart.listIterator();

            /* Go through all scripts to start and look for those which can be started now */
            while (iter.hasNext()) {
                PipedScript s = iter.next();
                boolean canStart;

                /* Script can start if:
                 * - it is not piped from another script
                 * - it is piped from another script and the script was started
                 * - it waits for another script and the script is finished
                 */
                canStart = s.getPipeFrom() == null;
                canStart = s.getPipeFrom() != null && findStarted(s.getPipeFrom(), started) != null ? true : canStart;
                canStart = s.getWaitFor()  != null && !isDone(s.getWaitFor(), tostart, started) ? false : canStart;
                /* If cannot be started, try another one */
                if (!canStart) {
                    continue;
                }

                long remainTime = this.timeout != ScriptRunner.NO_TIMEOUT
                                               ?  this.timeout - (System.currentTimeMillis() - startTime) / 1000
                                               :  Long.MAX_VALUE;
                if (s.getTimeout() == ScriptRunner.NO_TIMEOUT || s.getTimeout() > remainTime) {
                    s.setTimeout(remainTime);
                }
                /* And stuff for #build() method */
                s.setBuildLogParent(buildLogElement);
                s.setBuildProperties(buildProperties);
                s.setProgress(progressIn);
                /* Pipe to the required script */
                if (s.getPipeFrom() != null) {
                    s.setInputProvider(findStarted(s.getPipeFrom(), started).getOutputReader());
                }

                /* Initialize the script */
                s.initialize();
                /* Now start the script and set its thread to the pool */
                threads.startThread(s, s.getID());

                /* And move it from tostart array into started array. Reset the iterator, which
                 * allows to run all scripts except those waiting for others */
                iter.remove();
                iter = tostart.listIterator();
                started.add(s);

                // !!!!!!
                // WINDOWS SPECIFIC HACK:
                // Under Windows (tested on Windows XP with SP3, but suppose that it affects all
                // lower versions as well) we have found problems when several commands are started
                // simultaneously - although the process terminates successfully (finish reports 0
                // status), the reading from STDOUT/STDERR of a process blocks forever ... It is later
                // caught by timeout killer, but the whole pipe does not finish correctly.
                // If this does not occur on Windows Vista (and higher], check just for Windows XP
                // and lower can be added.
                // Preventing very fast concurrent process spawning seems to fix it (tests are OK). But
                // if you still find such problem, use threads.join(). It will lead to horrible
                // performance of the pipe under the affected windows versions, but it should be safe.
                if (Util.isWindows()) {
                    threads.join(1000);
                }
            }

            /* All scripts which could be started up to now were started ...
             * Try to join some scripts */
            threads.join(1000);

            /* And check if some scripts were finished */
            iter = started.listIterator();
            while (iter.hasNext()) {
                PipedScript s = iter.next();

                /* Remove the script from 'started' map when finished and not required by any
                 * other script not started yet */
                if (s.isDone() && null == findPipedFrom(s.getID(), tostart)) {
                    s.initialize(); // re-init is supposed to clear the inner variables to save memory 
                    iter.remove();
                }
            }

            /* Sanity check - if running time > timeout, leave the loop with error message */
            if (System.currentTimeMillis() - startTime > this.timeout * 1000) {
                LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
                synchronized (buildLogElement) {
                    buildLogElement.setAttribute("error", "build timeout");
                }
                break;
            }
        }

        /* Wait for all scripts to finish (they may be killed by their own timeouts) */
        threads.join();
        for (PipedScript s : scripts) {
             s.initialize(); // re-init is supposed to clear the inner variables to save memory
        }

        /* Set the time it took to exec command */
        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((System.currentTimeMillis() - startTime)));
        /* Go through children (individual commands), and check if there is an "error" attribute
         * in them. Copy it if so */
        for (Object e : buildLogElement.getChildren()) {
             Attribute a = ((Element) e).getAttribute("error");
             if (a != null) {
                 buildLogElement.setAttribute(a.detach());
                 break;
             }
        }

        //note: what other attributes/information should be stored in the element?
        //      ExecScript.setBuildLogHeader()????
        return buildLogElement;
    } // build

    /**
     * Execute the commands and return the results as XML
     */
    @Override
    public Element buildWithTarget(final Map<String, String> properties,
            final String target, final Progress progress)
            throws CruiseControlException {

        // final String origArgs = args;
        // try {
        // args = target;
        return build(properties, progress);
        // } finally {
        // args = origArgs;
        // }
    }

    /**
     * Sets the working directory where all the scripts are to be executed. Can be overridden
     * by the configuration of individual scripts.
     *
     * @param dir the directory where the command is to be executed
     */
    public void setWorkingDir(String dir) {
        this.workingDir = dir;
    } // setWorkingDir

    /**
     * Sets the working directory where all the scripts are to be executed. Can be overridden
     * by the configuration of individual scripts, but only by lower value.
     *
     * @param timeout build timeout in seconds
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    } // setWorkingDir

    /**
     * Should the STDOUT content of the scripts be kept gzipped within the builder? It may save
     * some memory required by CruiseControl in cases that data piped through scripts are huge, but
     * compressible. Can be overridden by the configuration of individual scripts, see
     * {@link Script#setGZipStdout(boolean)}.
     *
     * @param gzip <code>true</code> if STDOUT is required to be stored gzipped, <code>false</code>
     *   if raw STDOUT contents are kept.
     */
    public void setGZipStdout(boolean gzip) {
        this.gzip = gzip;
    } // setGZipStdout

    /**
     * Is the STDOUT content of the scripts in binary form? If <code>true</code>, the STDOUT is not
     * logged even in debug mode. If <code>false</code>, the STDOUT of the scripts will be logged in
     * debug mode. Can be overridden by the configuration of individual scripts, see
     * {@link Script#setBinaryStdout(boolean)}.
     *
     * @param binary <code>true</code> if STDOUT is in binary form, <code>false</code>
     *   if STDOUT is text.
     */
    public void setBinaryStdout(boolean binary) {
        this.binary = binary;
    } // setBinaryStdout

    /**
     * Creates object into which <code><exec /></code> tag will be set. Each call returns new
     * object which is expected to be set by CC. The attribute is not required; if not
     * specified, nothing will be executed.
     *
     * @return new {@link Script} object to configure.
     */
    @Cardinality(min = 0, max = -1)
    @ManualChildName("ExecBuilder")
    public PipedScript createExec() {
        scripts.add(new Script());
        return scripts.getLast();
    } // createExec

    /**
     * Adds object into the builder. It is similar to {@link #createExec()}, but allows to add any
     * 3rd party plugin implementing the {@link PipedScript} interface.
     *
     * @param execobj the implementation of {@link PipedScript} interface.
     */
    @SkipDoc // TODO: should be documented???
    public void add(PipedScript execobj) {
        scripts.add(execobj);
    } // add


    /**
     * Finds script with the given ID in the array of scripts not started yet.
     *
     * @param id the ID of the script to look for.
     * @param tostart the list of scripts to be started.
     * @return the instance of {@link Script} or <code>null</code> if not found.
     */
    private static PipedScript findToStart(String id, List<PipedScript> tostart) {
        for (PipedScript s : tostart) {
            if (id != null && id.equals(s.getID())) {
                return s;
            }
        }
        /* No such found */
        return null;
    } // findToStart
    /**
     * Checks. if there is a script in {@link #scripts} array which is required to be piped
     * to script with given ID.
     *
     * @param id the ID of the script to look for.
     * @param scripts the list of scripts to be looked it.
     * @return the instance of {@link Script} or <code>null</code> if not found.
     */
    private static PipedScript findPipedFrom(String id, List<PipedScript> scripts) {
         for (PipedScript s : scripts) {
             if (id != null && id.equals(s.getPipeFrom())) {
                return s;
             }
         }
         /* No such found */
        return null;
    } // findPipedFrom
    /**
     * Finds script with the given ID among those started (script is 'started' either if it is
     * running, or it is finished but piped to other script not started yet).
     *
     * @param id the ID of the script to look for.
     * @param started the list of scripts which were started.
     * @return the instance of {@link Script} or <code>null</code> if not found.
     */
    private static PipedScript findStarted(String id, List<PipedScript> started) {
        for (PipedScript s : started) {
            if (s.getID().equals(id)) {
                return s;
            }
        }
        /* No such found */
        return null;
    } // findStarted
    /**
     * Checks if script with the given ID is finished or not.
     *
     * @param id the ID of the script to look for.
     * @param tostart the list of scripts to be started (see {@link #findToStart(String, List)}
     *        for more details about what does 'tostart' mean).
     * @param started the list of scripts which were started (see {@link #findStarted(String, List)}
     *        for more details about what does 'started' mean).
     * @return <code>true</code> if the script is done, <code>false</code> if it has not been
     *         started yet, still running.
     */
    private static boolean isDone(String id, List<PipedScript> tostart, List<PipedScript> started) {
        /* Not started yet */
        if (findToStart(id, tostart) != null) {
            return false;
        }

        PipedScript script = findStarted(id, started);
        /* Not among started => finished */
        if (script == null) {
            return true;
        }

        return script.isDone();
    } // isDone

    /**
     * Method used for the detection of loops in piped commands. It works with two sets. First,
     * the set of script IDs which are not in loop (they lead to a script not piped or not
     * waiting for another script). The second is the set of scripts under check, but it cannot
     * be determined yet, if they are in loop.
     *
     * The algorithm is as follows:
     * <ul>
     *    <li> if the script is already in 'not-in-loop' set, return immediately
     *    <li> if the script is not piped from another script, neither it is waiting for another
     *         script, put it into 'not-in-loop' set and return
     *    <li> if the script is piped from another script, or it is waiting for another script,
     *         put it into 'checking' set and check recursively the scripts which this depends
     *         on
     *    <li> if the script is found in 'checking' set, loop is detected (it is checked twice
     *         during the recursive calls)
     *    <li> if none of the predecessors is in loop (recursion is left), move the script from
     *         'checking' to 'not-in-loop' set and return.
     * </ul>
     *
     * @param s the script to check
     * @param notInLoop the 'not-in-loop' set
     * @param checking the 'checking' set
     * @return 'not-in-loop' set with the ID of current script added when it is not in a loop
     * @throws CruiseControlException if loop is detected.
     */
    private Set<String> checkLoop(final PipedScript s, Set<String> notInLoop, Set<String> checking)
            throws CruiseControlException {
        final String pipeFrom = s.getPipeFrom();
        final String waitFor = s.getWaitFor();
        final String id = s.getID();

        /* Already determined not in loop */
        if (notInLoop.contains(id)) {
            return notInLoop;
        }
        /* Not piped and not waiting - cannot create loop */
        if (pipeFrom == null && waitFor == null) {
            notInLoop.add(id);
            return notInLoop;
        }

        /* If piped, check recursively the piped sequence */
        if (pipeFrom != null) {
            /* If the predecessor is in checking set, loop is detected! */
            if (checking.contains(pipeFrom)) {
                throw new CruiseControlException("Loop detected, ID " + id + " is within loop");
            }
            /* Cannot detect loop now, check the predecessor */
            checking.add(id);
            notInLoop = checkLoop(findToStart(pipeFrom, scripts), notInLoop, checking);
        }
        /* If waiting, check recursively as well */
        if (waitFor != null) {
            /* Predecessor in checking set, loop detected! */
            if (checking.contains(waitFor)) {
                throw new CruiseControlException("Loop detected, ID " + id + " is within loop");
            }
            /* Cannot detect loop now, ... */
            checking.add(id);
            notInLoop = checkLoop(findToStart(waitFor, scripts), notInLoop, checking);
        }

        /* Exception was not thrown, not in loop */
        checking.remove(id);
        notInLoop.add(id);
        return notInLoop;
    } // checkLoop

    /**
     * Finds all scripts piped from the given script
     *
     * @param id the ID of the script to look for.
     * @param scripts the list of scripts to be looked it.
     * @return collection of scripts piped to the given script
     */
    private static Collection<PipedScript> findPipedSeq(String id, List<PipedScript> scripts) {
        Collection<PipedScript> piped = new HashSet<PipedScript>(10);
        PipedScript found;

        /* Copy the collection, since data will be removed from it */
        scripts = new ArrayList<PipedScript>(scripts);

        while ((found = findPipedFrom(id, scripts)) != null) {
                /* If already in sequence, ignore */
                if (piped.contains(found)) {
                    continue;
                }
                scripts.remove(found);
                /* Add the piped and find those piped to it */
                piped.add(found);
                piped.addAll(findPipedSeq(found.getID(), scripts));
        }
        /* Get the sequence */
        return piped;
    } // findPipedSeq

    /** Wrapper for {@link #mergeEnv(OSEnvironment)}, just calling the wrapped method. It
     * is required for {@link #mergeEnv(OSEnvironment)} be callable from by Script class, 
     * since it contains the method with the same name */
    private void mergeEnv_wrap(final OSEnvironment env) {
        super.mergeEnv(env);
    }
    
    /* ----------- NESTED CLASSES ----------- */

    /**
     * Class for the configuration script to execute. It has the same arguments as
     * {@link ExecBuilder}, plus the ID of script, the ID of script from which it is supposed to
     * read data through STDIN (optional), and the ID of script which the current should wait
     * for.
     *
     * The class is the implementation of {@link Runnable} interface, as several scripts piped
     * one with another are started simultaneously.
     */
    @Description("Standard exec builder extended with attributes required for a builder to be piped "
            + "into the pipedexec builder. ")
    public final class Script extends PipedScriptBase implements PipedScript {

        /**
         * Override of {@link ScriptRunner} piping STDIN and STDOUT from/to other scripts
         * @author dtihelka
         */
        private final class PipedScriptRunner extends ScriptRunner {
            /**
             * Disable script consumption of STDOUT - although errors cannot be found in it now,
             * it is expected that errors are printed to STDERR when a sequence of piped
             * commands is started. Also, STDOUT of the script may contain binary data - it is
             * generally bad idea pass through text-expected classes.
             */
            @Override
            protected boolean letConsumeOut() {
                return false;
            }

            /**
             * Returns the consumer printing STDOUT of the script on
             * {@link org.apache.log4j.Level#DEBUG} level.
             */
            @Override
            protected StreamConsumer getDirectOutLogger() {
                /* Log only non-binary output */
                if (Boolean.FALSE.equals(getBinaryOutput())) {
                    return StreamLogger.getDebugLogger(ScriptRunner.LOG);
                }
                /* Disable logging otherwise */
                return new StreamConsumer() {
                    @Override
                    public void consumeLine(final String arg0) { /* Ignore data */ }
                };
            }

            /**
             * Assign STDOUT of the process directly to the StdoutBuffer (as byte stream) in
             * addition to the (text) consumer given.
             */
            @Override
            protected StreamPumper getOutPumper(final Process p, final StreamConsumer consumer) {
                return new StreamPumper(p.getInputStream(), getBinaryOutput().booleanValue(), consumer,
                        getOutputBuffer());
            } // getPumperOut
        }

        /** The override of {@link ExecBuilder} class customising {@link ExecBuilder#createScriptRunner()}
         *  and {@link Builder#mergeEnv(OSEnvironment)} methods; see their description for further
         *  details. */
        private final ExecBuilder builder = new ExecBuilder() {
            /** Returns script runner which does not allow to consume STDOUT, and it logs STDOUT in
             *  debug mode only (the output is passed to the piped script, and it may be huge, or it may
             *  contain binary data ...) */
            @Override
            protected ScriptRunner createScriptRunner() {
                return new PipedScriptRunner();
            } // createScriptRunner
            /** Override of {@link #mergeEnv(OSEnvironment)}, merging ENV set to {@link PipedExecBuilder}
             *  first and then ENV set to the script itself. */
            @Override
            protected void mergeEnv(final OSEnvironment env) {
                mergeEnv_wrap(env);
                super.mergeEnv(env);
            }
            /** Serialization UID */
            private static final long serialVersionUID = 2452456256173465623L;
        };

        @Override
        public void validate() throws CruiseControlException {
            super.validate();
            builder.validate();
        }

        @Override
        protected Element build() throws CruiseControlException {
            return builder.build(getBuildProperties(), getProgress(), getInputProvider());
        }

        @Override
        protected Logger log() {
            return ExecBuilder.LOG;
        }

        /** Just caller of {@link ExecBuilder#setTimeout(long)} */
        @Override
        public void setTimeout(long time) {
            builder.setTimeout(time);
        }
        /** Just caller of {@link ExecBuilder#getTimeout()} */
        @Override
        public long getTimeout() {
            return builder.getTimeout();
        }

        /** Just caller of {@link ExecBuilder#setWorkingDir(String)} */
        @Override
        public void setWorkingDir(String workingDir) {
            builder.setWorkingDir(workingDir);
        }
        /** Just caller of {@link ExecBuilder#getWorkingDir()} */
        @Override
        public String getWorkingDir() {
            return builder.getWorkingDir();
        }

        /** Raw caller of {@link ExecBuilder#setCommand(String)} for the script configuration
         *  purposes */
        @SuppressWarnings("javadoc")
        public void setCommand(String cmd) {
            this.builder.setCommand(cmd);
        }
        /** Raw caller of {@link ExecBuilder#setArgs(String)} for the script configuration
         *  purposes */
        @SuppressWarnings("javadoc")
        public void setArgs(String args) {
            this.builder.setArgs(args);
        }
        /** Raw caller of {@link ExecBuilder#setErrorStr(String)} for the script configuration
         *  purposes. */
        @SuppressWarnings("javadoc")
        public void setErrorStr(String errStr) {
            this.builder.setErrorStr(errStr);
        } // setErrorStr

        /** Raw caller of {@link ExecBuilder#createEnv()} for the script configuration
         *  purposes. */
        @SuppressWarnings("javadoc")
        public EnvConf createEnv() {
            return builder.createEnv();
        } // createEnv

        /** Prints string representation of the object */
        @Override
        public String toString() {
            return getClass().getName() + "[ID " + getID() + ", piped from "
                    + (getPipeFrom() != null ? getPipeFrom() : "-") + ", wait for "
                    + (getWaitFor() != null ? getWaitFor() : "-") + " Command: "
                    + builder.getCommand() + ' ' + builder.getArgs() + "]";
        }

    } // PipedExecScript

    /**
     * Simple class with pool of started threads. It implements {@link #join()} method waiting
     * for any (or all) threads in the pool.
     */
    private class ThreadPool {

        /** The list of threads in the pool. */
        private final List<Thread> threads = new ArrayList<Thread>();

        /**
         * Creates and <b>starts</b> new thread with the given {@link Runnable} implementation.
         * @param runnable the implementation of {@link Runnable} to start.
         * @param name the name of the thread.
         */
        void startThread(Runnable runnable, String name) {
            final Thread t = new Thread(runnable, name + " build thread");
            t.start();
            threads.add(t);
        }

        /**
         * Waits at least some time for some threads to die.
         * @param millis the number of milliseconds to wait.
         */
        void join(long millis) {
            /* Remove the threads not being alive */
            for (int i = threads.size() - 1; i >= 0; i--) {
                 if (!threads.get(i).isAlive()) {
                     threads.remove(i);
                 }
            }
            /* And try to join the others */
            millis = millis / (threads.size() > 0 ? threads.size() : 1);
            for (Thread t : threads) {
                try {
                    t.join(millis < 10 ? 10 : millis);
                } catch (InterruptedException e) {
                    /* Did not die in the given time ... */
                }
            }
        }
        /**
         * Waits for all threads to die.
         */
        void join() {
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    /* Should not happen */
                }
            }
            threads.clear();
        }
    } // ThreadPool


} // PipedExecBuilder

