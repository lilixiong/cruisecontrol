/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardUtils;

import org.springframework.web.servlet.ModelAndView;

public class BuildListingController extends BaseMultiActionController {
    private final HistoricalBuildSummariesService buildSummariesService;

    private final BuildSummaryUIService buildSummaryUIService;

    public BuildListingController(HistoricalBuildSummariesService buildSummariesService,
            BuildSummaryUIService buildSummaryUIService) {
        this.buildSummariesService = buildSummariesService;
        this.buildSummaryUIService = buildSummaryUIService;
        this.setSupportedMethods(new String[] {"GET"});
    }

    public ModelAndView all(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List all = buildSummariesService.getAll(getProjectName(request));
        return process(getProjectName(request), "page_all_builds", buildSummaryUIService.transform(all));
    }

    public ModelAndView passed(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List allSucceed = buildSummariesService.getAllSucceed(getProjectName(request));
        return process(getProjectName(request), "page_all_successful_builds",
                buildSummaryUIService.transform(allSucceed));
    }

    private ModelAndView process(String projectName, String viewname, List list) {
        Map model = new HashMap();
        model.put("buildCmds", list);
        model.put("projectName", projectName);
        return new ModelAndView(viewname, model);
    }

    private String getProjectName(HttpServletRequest request) {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = DashboardUtils.decode(url[url.length - 1]);
        return projectName;
    }
}
