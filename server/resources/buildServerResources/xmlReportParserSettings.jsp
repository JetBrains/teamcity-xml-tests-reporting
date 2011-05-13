<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
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
<jsp:useBean id="reportTypeForm" scope="request" class="jetbrains.buildServer.xmlReportPlugin.ReportTypeForm"/>

<c:set var="reportType"
       value="${propertiesBean.properties['xmlReportParsing.reportType']}"/>

<c:set var="displayReportsSettings"
       value="${not empty reportType ? true : false}"/>

<c:set var="displayInspectionsSettings"
       value="${reportType == 'findBugs' ||
                reportType == 'pmd' ||
                reportType == 'checkstyle' ||
                reportType == 'jslint' ? true : false}"/>

<c:set var="displayFindBugsSettings"
       value="${reportType == 'findBugs' ? true : false}"/>


<c:if test="${reportType == 'junit' || reportType == 'nunit' || reportType == 'surefire'|| reportType == 'mstest'}">
  <c:set var="displayWarning"
         value="true"/>
</c:if>

<tr class="noBorder" id="xmlReportParsing.warning.container" style="${empty displayWarning ? 'display:none;' : ''}">
    <td colspan="2">
        <div class="attentionComment">
            Please make sure that tests are not detected automatically before using this feature.
        </div>
    </td>
</tr>
<tr class="noBorder" id="xmlReportParsing.reportType.container">
    <th><label for="xmlReportParsing.reportType">Report type:</label></th>
    <td>
        <c:set var="onchange">
          var selectedValue = this.value;
          if (selectedValue == '') {
          BS.Util.hide($('xmlReportParsing.reportDirs.container'));
          BS.Util.hide($('xmlReportParsing.verboseOutput.container'));
          } else {
          BS.Util.show($('xmlReportParsing.reportDirs.container'));
          BS.Util.show($('xmlReportParsing.verboseOutput.container'));
          BS.MultilineProperties.show('xmlReportParsing.reportDirs', true);
          $('xmlReportParsing.reportDirs').focus();
          }
          var isInspection = (selectedValue == 'findBugs' ||
          selectedValue == 'pmd' ||
          selectedValue == 'checkstyle' ||
          selectedValue == 'jslint');
          if (isInspection) {
          BS.Util.show($('xmlReportParsing.max.errors.container'));
          BS.Util.show($('xmlReportParsing.max.warnings.container'));
          } else {
          BS.Util.hide($('xmlReportParsing.max.errors.container'));
          BS.Util.hide($('xmlReportParsing.max.warnings.container'));
          }
          if (selectedValue == 'findBugs') {
          BS.Util.show($('xmlReportParsing.findBugs.home.container'));
          } else {
          BS.Util.hide($('xmlReportParsing.findBugs.home.container'));
          }

          if (selectedValue == 'junit'
          || selectedValue == 'nunit'
          || selectedValue == 'surefire'
          || selectedValue == 'mstest') {
          BS.Util.show($('xmlReportParsing.warning.container'));
          } else {
          BS.Util.hide($('xmlReportParsing.warning.container'));
          }

          BS.MultilineProperties.updateVisible();
        </c:set>
        <props:selectProperty name="xmlReportParsing.reportType"
                              onchange="${onchange}">
            <c:set var="selected" value="false"/>
            <c:if test="${empty reportType}">
                <c:set var="selected" value="true"/>
            </c:if>
            <props:option value="" selected="${selected}">&lt;Do not process&gt;</props:option>
            <c:forEach var="reportType" items="${reportTypeForm.availableReportTypes}">
                <c:set var="selected" value="false"/>
                <c:if test="${reportType.type == reportType}">
                    <c:set var="selected" value="true"/>
                </c:if>
                <props:option value="${reportType.type}"
                              selected="${selected}"><c:out value="${reportType.displayName}"/></props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="smallNote">Choose report type.</span>
    </td>
</tr>

<tr class="noBorder" id="xmlReportParsing.findBugs.home.container"
    style="${displayFindBugsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.findBugs.home">FindBugs home path:</label></th>
    <td><props:textProperty name="xmlReportParsing.findBugs.home" className="longField"/>
        <span class="smallNote">Path to FindBugs installation on agent. This path is used for loading bug patterns names and descriptions.</span>
    </td>
</tr>

<tr class="noBorder" id="xmlReportParsing.reportDirs.container"
    style="${displayReportsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.reportDirs">Monitoring rules:</label></th>
    <td>
        <props:multilineProperty name="xmlReportParsing.reportDirs" expanded="true" rows="5" cols="40"
                                 linkTitle="Type report directories"/>
    <span class="smallNote">
      New line or comma separated set of rules in the form <strong>of +|-:path</strong>.<br/>
      Support ant-style wildcards like <strong>dir/**/*.xml</strong>.
    </span>
    <span class="error" id="error_xmlReportParsing.reportDirs"></span>
    </td>
</tr>
<tr class="noBorder" id="xmlReportParsing.verboseOutput.container"
    style="${displayReportsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.verboseOutput">Verbose output:</label></th>
    <td>
        <props:checkboxProperty name="xmlReportParsing.verboseOutput"/>
    </td>
</tr>

<tr class="noBorder" id="xmlReportParsing.max.errors.container"
    style="${displayInspectionsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.max.errors">Maximum error limit:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.errors" style="width:6em;" maxlength="12"/>
        <span class="smallNote">Fail the build if the specified number of errors is exceeded.</span>
    </td>
</tr>


<tr class="noBorder" id="xmlReportParsing.max.warnings.container"
    style="${displayInspectionsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.max.warnings">Warnings limit:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.warnings" style="width:6em;" maxlength="12"/>
        <span class="smallNote">Fail the build if the specified number of warnings is exceeded. Leave blank if there is no limit.</span>
    </td>
</tr>

<script type="text/javascript">
  BS.MultilineProperties.setVisible('xmlReportParsing.reportDirs', true);
  BS.MultilineProperties.show('xmlReportParsing.reportDirs', true);
</script>