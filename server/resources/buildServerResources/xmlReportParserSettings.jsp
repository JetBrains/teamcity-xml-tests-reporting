<%@ taglib prefix="bs" tagdir="/WEB-INF/tags/" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%--
  ~ Copyright 2000-2013 JetBrains s.r.o.
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

<tr>
  <td colspan="2"><em>Allows importing data from report files produced by an external tool in TeamCity.</em> <bs:help file="XML+Report+Processing"/></td>
</tr>

<tr id="xmlReportParsing.warning.container" style="${empty displayWarning ? 'display:none;' : ''}">
    <td colspan="2">
        <div class="icon_before icon16 attentionComment">
            Please make sure that tests are not detected automatically before using this feature.
        </div>
    </td>
</tr>
<tr id="xmlReportParsing.reportType.container">
    <th><label for="xmlReportParsing.reportType">Report type: <l:star/></label></th>
    <td>
        <c:set var="onchange">
          var selectedValue = this.options[this.selectedIndex].value;
          if (selectedValue == '') {
          BS.Util.hide('xmlReportParsing.reportDirs.container');
          BS.Util.hide('xmlReportParsing.verboseOutput.container');
          } else {
          BS.Util.show('xmlReportParsing.reportDirs.container');
          BS.Util.show('xmlReportParsing.verboseOutput.container');
          BS.MultilineProperties.show('xmlReportParsing.reportDirs', true);
          $('xmlReportParsing.reportDirs').focus();
          }
          var isInspection = (selectedValue == 'findBugs' ||
          selectedValue == 'pmd' ||
          selectedValue == 'checkstyle' ||
          selectedValue == 'jslint');
          if (isInspection) {
          BS.Util.show('xmlReportParsing.condition.note.container');
          } else {
          BS.Util.hide('xmlReportParsing.condition.note.container');
          }
          if (selectedValue == 'findBugs') {
          BS.Util.show('xmlReportParsing.findBugs.home.container');
          } else {
          BS.Util.hide('xmlReportParsing.findBugs.home.container');
          }

          if (selectedValue == 'junit'
          || selectedValue == 'nunit'
          || selectedValue == 'surefire'
          || selectedValue == 'mstest') {
          BS.Util.show('xmlReportParsing.warning.container');
          } else {
          BS.Util.hide('xmlReportParsing.warning.container');
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
        <span class="smallNote">Choose a report type.</span>
      <span class="error" id="error_xmlReportParsing.reportType"></span>
    </td>
</tr>

<tr id="xmlReportParsing.findBugs.home.container"
    style="${displayFindBugsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.findBugs.home">FindBugs home path:</label></th>
    <td><props:textProperty name="xmlReportParsing.findBugs.home" className="longField"/>
        <span class="smallNote">Path to FindBugs installation on agent. This path is used for loading bug pattern names and descriptions.</span>
    </td>
</tr>

<tr id="xmlReportParsing.reportDirs.container"
    style="${displayReportsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.reportDirs">Monitoring rules: <l:star/></label></th>
    <td><c:set var="note">
      Newline- or comma-separated set of rules in the form <strong>of +|-:path</strong>.<br/>
      Ant-style wildcards supported, e.g. <strong>dir/**/*.xml</strong>
    </c:set
        ><props:multilineProperty name="xmlReportParsing.reportDirs" className="longField" expanded="true" rows="5" cols="40"
                                 linkTitle="Type report monitoring rules" note="${note}"/>
    </td>
</tr>
<tr id="xmlReportParsing.verboseOutput.container"
    style="${displayReportsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.verboseOutput">Verbose output:</label></th>
    <td>
        <props:checkboxProperty name="xmlReportParsing.verboseOutput"/>
    </td>
</tr>

<c:set var="noLimits"
       value="${empty propertiesBean.properties['xmlReportParsing.max.errors'] and empty propertiesBean.properties['xmlReportParsing.max.warnings']}"/>

<tr id="xmlReportParsing.condition.note.container" style="${displayInspectionsSettings ? '' : 'display:none;'}">
  <td colspan="2">
    <c:set var="editFailureCondLink"><admin:editBuildTypeLink step="buildFailureConditions" buildTypeId="${buildForm.settings.externalId}" withoutLink="true"/></c:set>
    You can configure a build to fail if it has too many inspection errors or warnings by
    adding a corresponding <a href="${editFailureCondLink}#addFeature=BuildFailureOnMetric">build failure condition</a>.<br/>
    To configure error and warning limits for current monitoring rules only, use the
    <c:choose>
      <c:when test="${noLimits}">
        <a href="#"
        onclick="BS.Util.show('xmlReportParsing.max.errors.container');
        BS.Util.show('xmlReportParsing.max.warnings.container');
        BS.MultilineProperties.updateVisible();
        return false;">following settings</a>
      </c:when>
      <c:otherwise>following settings</c:otherwise>
    </c:choose>
  </td>
</tr>

<tr id="xmlReportParsing.max.errors.container"
    style="${noLimits ? 'display: none;' : ''}">
    <th><label for="xmlReportParsing.max.errors">Maximum error count:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.errors" style="width:6em;" maxlength="12"/>
        <span class="smallNote">Fail a build if the specified number of errors is exceeded. Leave blank if there is no limit.</span>
    </td>
</tr>


<tr id="xmlReportParsing.max.warnings.container"
    style="${noLimits ? 'display: none;' : ''}">
    <th><label for="xmlReportParsing.max.warnings">Maximum warning count:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.warnings" style="width:6em;" maxlength="12"/>
        <span class="smallNote">Fail a build if the specified number of warnings is exceeded. Leave blank if there is no limit.</span>
    </td>
</tr>

<script type="text/javascript">
  BS.MultilineProperties.setVisible('xmlReportParsing.reportDirs', true);
  BS.MultilineProperties.show('xmlReportParsing.reportDirs', true);
</script>