<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 2000-2011 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    XML report processing:
    <c:choose>
        <c:when test="${empty propertiesBean.properties['xmlReportParsing.reportType']}">
            <strong>disabled</strong>
        </c:when>
        <c:otherwise>
            <strong>enabled</strong>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty propertiesBean.properties['xmlReportParsing.reportType']}">
    <div class="parameter">
        Report type: <props:displayValue name="xmlReportParsing.reportType"
                                         emptyValue="none specified"/>
    </div>

    <c:if test="${propertiesBean.properties['xmlReportParsing.reportType'] == 'findBugs'}">
        <div class="parameter">
            FindBugs home path:
            <c:choose>
                <c:when test="${not empty propertiesBean.properties['xmlReportParsing.findBugs.home']}">
                    <props:displayValue name="xmlReportParsing.findBugs.home"
                                        emptyValue="none specified"/>
                </c:when>
                <c:otherwise>
                    <strong>no path specified, please specify value</strong>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>

    <div class="parameter">
        Report directories: <props:displayValue name="xmlReportParsing.reportDirs"
                                                emptyValue="none specified"/>
    </div>

    <div class="parameter">
        Verbose output:
        <c:choose>
            <c:when test="${propertiesBean.properties['xmlReportParsing.verboseOutput']}">
                <strong>enabled</strong>
            </c:when>
            <c:otherwise>
                <strong>disabled</strong>
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${propertiesBean.properties['xmlReportParsing.reportType'] == 'findBugs' ||
                  propertiesBean.properties['xmlReportParsing.reportType'] == 'pmd' ||
                  propertiesBean.properties['xmlReportParsing.reportType'] == 'checkstyle'}">
        <div class="parameter">
            Maximum error limit:
            <c:choose>
                <c:when test="${not empty propertiesBean.properties['xmlReportParsing.max.errors']}">
                    <props:displayValue name="xmlReportParsing.max.errors"
                                        emptyValue="none specified"/>
                </c:when>
                <c:otherwise>
                    <strong>no limit</strong>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="parameter">
            Warnings limit:
            <c:choose>
                <c:when test="${not empty propertiesBean.properties['xmlReportParsing.max.warnings']}">
                    <props:displayValue name="xmlReportParsing.max.warnings"
                                        emptyValue="none specified"/>
                </c:when>
                <c:otherwise>
                    <strong>no limit</strong>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>
</c:if>