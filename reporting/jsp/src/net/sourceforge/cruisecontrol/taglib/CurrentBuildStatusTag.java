/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

package net.sourceforge.cruisecontrol.taglib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import javax.servlet.jsp.JspException;

public class CurrentBuildStatusTag extends CruiseControlTagSupport {

    public int doEndTag() throws JspException {
        writeStatus(getPageContext().getOut());
        return EVAL_PAGE;
    }

    private void writeStatus(java.io.Writer out) throws JspException {
        BufferedReader br = null;
        File logDir = findLogDir();

        String currentBuildFileName = getContextParam("currentBuildStatusFile");
        if (currentBuildFileName == null) {
            throw new JspException("You need to declare the current build file");
        }
        File currentBuildFile = new File(logDir, currentBuildFileName);
        if (!currentBuildFile.exists()) {
            return;
        }
        try {
            br = new BufferedReader(new FileReader(currentBuildFile));
            String s = br.readLine();
            while (s != null) {
                out.write(s);
                s = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new JspException("Error reading status file: " + currentBuildFileName + " : " + e.getMessage());
        } finally {
            try {
               if (br != null) {
                   br.close();
               }
            } catch (IOException e) {
                e.printStackTrace();
            }
            br = null;
        }
    }
}
