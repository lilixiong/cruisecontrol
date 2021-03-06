/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

public final class FileUtil {

    private static final Logger LOG = Logger.getLogger(FileUtil.class);

    private FileUtil() { }

    public static byte[] getFileAsBytes(final File file) throws IOException {
        final InputStream is = new FileInputStream(file);
        final byte[] bytes;
        try {

            // Get the size of the file
            final long length = file.length();

            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                // File is too large
                throw new IOException("File too large. File size is " + length + ". Maximum allowed is "
                        + Integer.MAX_VALUE);
            }

            // Create the byte array to hold the data
            bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } finally {
            // Close the input stream and return bytes
            is.close();
        }
        return bytes;
    }

    public static void bytesToFile(final byte[] data, final File outFile) {
        try {
            final FileOutputStream fos = new FileOutputStream(outFile);
            try {
                final ObjectOutputStream oos = new ObjectOutputStream(fos);
                try {
                    oos.writeObject(data);
                } finally {
                    oos.close();
                }
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            final String message = "Error creating output file: " + outFile.getAbsolutePath();
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
        }
    }


    /**
     * Used to get a File reference to a resource. Assumes the resource will NOT be found in a jar,
     * but instead found on disk.
     * @param resourceName the name of the resource to find, ie, the file name
     * @return a file pointing to the location on disk of the resource.
     */
    public static File getFileFromResource(final String resourceName) {
        if (resourceName == null) {
            throw new IllegalArgumentException("resourceName should not be null");
        }

        final URL configURL = FileUtil.class.getClassLoader().getResource(resourceName);
        if (configURL == null) {
            throw new RuntimeException("Could not find resource: " + resourceName);
        }

        final URI configURI;
        try {
             configURI = new URI(configURL.toExternalForm());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error getting URI for resource: " + resourceName
                    + ", URL: " + configURL.toExternalForm(), e);
        }

        return new File(configURI.getPath());
    }
}
