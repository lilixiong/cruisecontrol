/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;

public class FileSystemTest extends TestCase {
    private static final int ONE_SECOND = 1000;
    private FileSystem fs;
    private File tempDirectory;
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected void setUp() throws Exception {
        fs = new FileSystem();

        File javaTempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirectory = new File(javaTempDir, "filesystemtest" + System.currentTimeMillis());
        filesToDelete.add(tempDirectory);
        tempDirectory.mkdir();
        
        fs.setFolder(tempDirectory.getAbsolutePath());
    }

    protected void tearDown() {
        filesToDelete.delete();
        fs = null;
        tempDirectory = null;
    }

    public void testValidateFailsWhenFolderNotSet() {
        fs = new FileSystem();

        try {
            fs.validate();
            fail("FileSystem should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'folder' is required for FileSystem", e.getMessage());
        }
    }
    
    public void testValidateFailsWhenFolderDoesNotExist() {
        assertTrue("problem deleting tempDir", tempDirectory.delete());
        
        try {
            fs.validate();
            fail("FileSystem should throw exceptions when folder doesn't exist.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().indexOf("must exist") > -1);
        }
    }


    public void testValidatePasssWhenFolderSetAndExists() throws CruiseControlException {
        fs.validate();
    }

    public void testGettingModifications() throws Exception {

        // Check for modifications...there shouldn't be any
        final Date startTime = new Date(tempDirectory.lastModified() + 1);
        final Date timeOne = new Date(startTime.getTime() + 2 * ONE_SECOND);
        final Date timeTwo = new Date(timeOne.getTime() + 2 * ONE_SECOND);
        final Date timeThree = new Date(timeTwo.getTime() + 2 * ONE_SECOND);
        List<Modification> mods = fs.getModifications(startTime, timeOne);
        assertNotNull(mods);
        assertEquals(0, mods.size());
        assertEquals(0, fs.getProperties().size());

        writeNewFile(timeOne, "testing");
        writeNewFile(timeOne, "testing2");

        // Check for mods...there should be some, one for each file written.
        mods = fs.getModifications(startTime, timeOne);
        assertNotNull(mods);
        assertTrue(mods.size() >= 2);
        assertEquals(0, fs.getProperties().size());

        writeNewFile(timeTwo, "testing3");
        writeNewFile(timeTwo, "testing4");
        writeNewFile(timeTwo, "testing5");

        // Checking for mods again should turn up only the new files.
        fs.setProperty("property");
        mods = fs.getModifications(timeOne, timeTwo);
        assertNotNull(mods);
        assertTrue(mods.size() >= 3);
        final Map<String, String> properties = fs.getProperties();
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("property"));

        final File tempFile = writeNewFile(timeThree, "testing6");

        // Checking for mods again should turn up only the one file
        mods = fs.getModifications(timeTwo, timeThree);
        assertNotNull(mods);
        assertTrue(mods.size() >= 1);

        // Using this one mod, check the modification information for
        // correctness.
        final Modification modification = mods.get(0);
        assertEquals(tempFile.getName(), modification.getFileName());
        assertEquals(tempFile.getParent(), modification.getFolderName());
        assertEquals(tempFile.lastModified(), modification.modifiedTime.getTime());
    }
    
    public void testShouldReturnModificationsIfTheLastModifiedTimeChanged() {
        fs.setIncludeDirectories(true);
        final Date lastBuildTime = new Date(tempDirectory.lastModified() + 2 * ONE_SECOND);
        final Date checkTime = new Date(lastBuildTime.getTime() + 2 * ONE_SECOND);
        
        List<Modification> mods = fs.getModifications(lastBuildTime, checkTime);
        assertEquals(0, mods.size());
        
        final long timeBetweenLastBuildAndCheckTime = lastBuildTime.getTime() + ONE_SECOND;
        tempDirectory.setLastModified(timeBetweenLastBuildAndCheckTime);
        
        mods = fs.getModifications(lastBuildTime, checkTime);

        assertEquals(1, mods.size());
    }
    
    public void testShouldOnlyIncludeDirectoriesIfOptionIsSet() {
        long directoryModifiedTime = tempDirectory.lastModified();
        final Date earlierTime = new Date(directoryModifiedTime - 2 * ONE_SECOND);
        
        List<Modification> mods = fs.getModifications(earlierTime, null);
        assertEquals(0, mods.size());
        
        fs.setIncludeDirectories(true);
        mods = fs.getModifications(earlierTime, null);
        assertEquals(1, mods.size());        
    }

    private File writeNewFile(final Date modifiedTime, final String content)
            throws IOException, CruiseControlException {
        
        final File tempFile = File.createTempFile("CruiseControl", "TEST", tempDirectory);
        filesToDelete.add(tempFile);
        IO.write(tempFile, content);
        tempFile.setLastModified(modifiedTime.getTime());
        return tempFile;
    }
}