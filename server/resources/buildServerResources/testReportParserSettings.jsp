<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="reportTypeForm" scope="request" class="jetbrains.buildServer.testReportParserPlugin.ReportTypeForm"/>
<l:settingsGroup title="Test Reports Settings">

  <tr id="testReportParsing.enabled.container">
    <th><label for="testReportParsing.enabled">Test reports:</label></th>
    <td>
      <c:set var="onclick">
        $('testReportParsing.reportType').disabled = !this.checked;
        $('testReportParsing.reportDirs').disabled = !this.checked;
        $('testReportParsing.verboseOutput').disabled = !this.checked;
      </c:set>
      <props:checkboxProperty name="testReportParsing.enabled" onclick="${onclick}"/>
      <label for="testReportParsing.enabled">Enable test report monitoring</label>
      <span class="smallNote">Tests will be logged as soon as report files appear.</span>
    </td>
  </tr>

  <tr id="testReportParsing.reportType.container">
    <th><label for="testReportParsing.reportType">Import data from:</label></th>
    <td>
      <c:set var="onchange">
        <%--update--%>
      </c:set>
      <props:selectProperty name="testReportParsing.reportType"
                            disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"
                            onchange="${onchange}">
        <c:set var="selected" value="false"/>
        <c:if test="${empty propertiesBean.properties['testReportParsing.reportType']}">
          <c:set var="selected" value="true"/>
        </c:if>
        <props:option value="" selected="${selected}">--Choose report type--</props:option>
        <c:forEach var="reportType" items="${reportTypeForm.availableReportTypes}">
          <c:set var="selected" value="false"/>
          <c:if test="${reportType.type == propertiesBean.properties['testReportParsing.reportType']}">
            <c:set var="selected" value="true"/>
          </c:if>
          <props:option value="${reportType.type}"
                        selected="${selected}"><c:out value="${reportType.displayName}"/></props:option>
        </c:forEach>
      </props:selectProperty>
      <span class="smallNote">Choose report format.</span>
    </td>
  </tr>
</l:settingsGroup>

<jsp:include page="${reportTypeForm.selectedReportType.reportParserSettingsJspPath}"/>
