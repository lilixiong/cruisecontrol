/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

import junit.framework.TestCase;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

public class AntScriptTest extends TestCase {
    private AntScript script;
    private AntBuilder unixBuilder;
    private AntBuilder windowsBuilder;
    private Hashtable properties;
    private static final boolean USE_LOGGER = true;
    private static final boolean USE_SCRIPT = true;
    private static final boolean IS_WINDOWS = true;
    private static final String UNIX_PATH = "/usr/java/jdk1.5.0/lib/tools.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/dist/cruisecontrol.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/log4j.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/jdom.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/xerces.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/xalan.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/jakarta-oro-2.0.3.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mail.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/junit.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/activation.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/commons-net-1.1.0.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/starteam-sdk.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-tools.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-remote.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/smack.jar:.";
    private static final String WINDOWS_PATH = "C:\\Progra~1\\IBM\\WSAD\\tools.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\dist\\cruisecontrol.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\log4j.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jdom.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xerces.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xalan.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jakarta-oro-2.0.3.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mail.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\junit.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\activation.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\commons-net-1.1.0.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\starteam-sdk.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-tools.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-remote.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\smack.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\comm.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\x10.jar;.";

    protected void setUp() throws Exception {
        script = new AntScript();
      
        
        properties = new Hashtable();
        properties.put("label", "200.1.23");
        
        // default setup of script
        script.setBuildProperties(properties);
        script.setArgs(new ArrayList());
        script.setProperties(new ArrayList());
        script.setBuildFile("buildfile");
        script.setTarget("target"); 
        
        unixBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return UNIX_PATH;
            }
        };
        unixBuilder.setTarget("target");
        unixBuilder.setBuildFile("buildfile");
        
        windowsBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return WINDOWS_PATH;
            }
        };
        windowsBuilder.setTarget("target");
        windowsBuilder.setBuildFile("buildfile");

        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%m%n")));
    }
    
    public void testGetAntLauncherJarLocationForWindows() throws Exception {
        assertEquals("C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar",
                     script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS));
    }

    public void testGetAntLauncherJarLocationForUnix() throws Exception {
        assertEquals("/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS));
    }
    
    public void testGetCommandLineArgs() throws CruiseControlException {
        String[] resultInfo =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertArray(
                "Logger set to INFO",
                resultInfo,
            script.buildCommandline().getCommandline());
       
    
        String[] resultLogger =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);      
        script.setUseScript(!USE_SCRIPT);
        TestUtil.assertArray(
                "Using result Logger",
                resultLogger,
            script.buildCommandline().getCommandline());
     
        
    }

    public void testGetCommandLineArgs_EmptyLogger() throws CruiseControlException {
        String[] resultInfo =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        properties.put("label", "");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        TestUtil.assertArray(
                "resultInfo",
                resultInfo,
            script.buildCommandline().getCommandline());
        
    
        String[] resultLogger =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        script.setUseLogger(USE_LOGGER);
        script.setUseScript(!USE_SCRIPT);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultLogger",
                resultLogger,
            script.buildCommandline().getCommandline());
        
    }

    public void testGetCommandLineArgs_Debug() throws CruiseControlException {
        String[] resultDebug =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-debug",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };

        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setUseDebug(true);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultDebug",
                resultDebug,
            script.buildCommandline().getCommandline());
       
    }

    public void testGetCommandLineArgs_DebugWithListener() throws CruiseControlException {
             String[] resultDebug =
             {
                 "java",
                 "-classpath",
                 script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                 "org.apache.tools.ant.launch.Launcher",
                 "-lib",
                 UNIX_PATH,
                 "-listener",
                 "org.apache.tools.ant.XmlLogger",
                 "-DXmlLogger.file=log.xml",
                 "-debug",
                 "-Dlabel=200.1.23",
                 "-buildfile",
                 "buildfile",
                 "target" };
             script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
             script.setBuildProperties(properties);
             script.setUseLogger(!USE_LOGGER);
             script.setWindows(!IS_WINDOWS);
             script.setUseScript(!USE_SCRIPT);
             script.setSystemClassPath(UNIX_PATH);
             script.setUseDebug(true);
        TestUtil.assertArray(
                     "debug with listener",
                     resultDebug,
            script.buildCommandline().getCommandline());
         }
    
    public void testGetCommandLineArgs_Quiet() throws CruiseControlException {
        String[] resultQuiet =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-quiet",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(UNIX_PATH);
        script.setUseQuiet(true);
        TestUtil.assertArray(
                "resultQuiet",
                resultQuiet,
            script.buildCommandline().getCommandline());
       
    }

    public void testGetCommandLineArgs_MaxMemory() throws CruiseControlException {
        String[] resultWithMaxMemory =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        List args = new ArrayList();
        args.add(arg);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultWithMaxMemory",
                resultWithMaxMemory,
            script.buildCommandline().getCommandline());
        
    }

    public void testGetCommandLineArgs_MaxMemoryAndProperty() throws CruiseControlException {
        String[] resultWithMaxMemoryAndProperty =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-Dfoo=bar",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        Property prop = unixBuilder.createProperty();
        prop.setName("foo");
        prop.setValue("bar");

        List args = new ArrayList();
        args.add(arg);
        List props = new ArrayList();
        props.add(prop);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setProperties(props);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultWithMaxMemoryAndProperty",
                resultWithMaxMemoryAndProperty,
            script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_BatchFile() throws CruiseControlException {
        String[] resultBatchFile =
            {
                "ant.bat",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setAntScript("ant.bat");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);
        TestUtil.assertArray(
                "resultBatchFile",
                resultBatchFile,
            script.buildCommandline().getCommandline());
      
    }

    public void testGetCommandLineArgs_ShellScript() throws CruiseControlException {
        String[] resultShellScript =
            {
                "ant.sh",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setAntScript("ant.sh");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);
        TestUtil.assertArray(
                "resultShellScript",
                resultShellScript,
            script.buildCommandline().getCommandline());
      
    }

    public void testGetCommandLineArgs_AlternateLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-listener",
                "com.canoo.Logger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);


        TestUtil.assertArray(
                "args",
                args,
            script.buildCommandline().getCommandline());
       
    }

  
}
