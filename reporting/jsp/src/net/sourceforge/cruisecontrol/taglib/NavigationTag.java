/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 */
public class NavigationTag implements Tag, BodyTag {

    private Tag _parent;
    private BodyContent _bodyOut;
    private PageContext _pageContext;
    private File _logDir;
    private String[] _fileNames;
    private int _count;
    private DateFormat _dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private String _labelSeparator = "L";

    /**
     *
     */
    protected String getUrl(String fileName, String servletPath) {
        String queryString = fileName.substring(0, fileName.lastIndexOf(".xml"));
        return servletPath + "?" + queryString;
    }

    /**
     *
     */
    protected String getLinkText(String fileName) {
        String dateString = "";
        String label = "";
        if(fileName.lastIndexOf(_labelSeparator) > -1) {
            dateString = fileName.substring(3, fileName.lastIndexOf(_labelSeparator));
            label = " (" + fileName.substring(fileName.lastIndexOf(_labelSeparator) + 1, fileName.lastIndexOf(".xml")) + ")";
        } else {
            dateString = fileName.substring(3, fileName.lastIndexOf(".xml"));
        }
        DateFormat inputDate = null;
        if(dateString.length() == 14) {
            inputDate = new SimpleDateFormat("yyyyMMddHHmmss");
        } else {
            inputDate = new SimpleDateFormat("yyyyMMddHHmm");
        }

        Date date = null;
        try {
            date = inputDate.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return _dateFormat.format(date) + label;
    }

    protected String getServletPath() {
        String servletPath = ((HttpServletRequest) _pageContext.getRequest()).getServletPath();
        String contextPath = ((HttpServletRequest) _pageContext.getRequest()).getContextPath();
        return contextPath + servletPath;
    }

    /**
     *
     */
    public void setDateFormat(String dateFormat) {
        _dateFormat = new SimpleDateFormat(dateFormat);
    }

    public int doStartTag() throws JspException {
        _logDir = new File(_pageContext.getServletConfig().getInitParameter("logDir"));
        System.out.println("Scanning directory: " + _logDir.getAbsolutePath() + " for log files.");

        _fileNames = _logDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("log") && name.endsWith(".xml") && !(new File(dir, name).isDirectory());
            }
        });

        //sort links...
        Arrays.sort(_fileNames, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((String) o2).compareTo((String) o1);
            }
        });

        return EVAL_BODY_TAG;
    }

    public void doInitBody() throws JspException {
        if (_count < _fileNames.length) {
            _pageContext.setAttribute("url", getUrl(_fileNames[_count], getServletPath()));
            _pageContext.setAttribute("linktext", getLinkText(_fileNames[_count]));
            _count++;
        }
    }

    public int doAfterBody() throws JspException {
        if (_count < _fileNames.length) {
            _pageContext.setAttribute("url", getUrl(_fileNames[_count], getServletPath()));
            _pageContext.setAttribute("linktext", getLinkText(_fileNames[_count]));
            _count++;
            return EVAL_BODY_TAG;
        } else {
            try {
                _bodyOut.writeOut(_bodyOut.getEnclosingWriter());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return SKIP_BODY;
        }
    }

    public void release() {
    }

    public void setPageContext(PageContext pageContext) {
        _pageContext = pageContext;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    public void setParent(Tag parent) {
        _parent = parent;
    }

    public Tag getParent() {
        return _parent;
    }

    public void setBodyContent(BodyContent bodyOut) {
        _bodyOut = bodyOut;
    }
}