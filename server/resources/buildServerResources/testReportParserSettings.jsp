<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="reportTypeForm" scope="request" class="jetbrains.buildServer.testReportParserPlugin.ReportTypeForm"/>

<c:set var="displayJUnitSettings"
       value="${not empty propertiesBean.properties['testReportParsing.reportType'] ? true : false}"/>
<c:set var="displayFindBugsSettings"
       value="${propertiesBean.properties['testReportParsing.reportType'] == 'findBugs' ? true : false}"/>
<%--<c:set var="displayNUnitSettings" value="${propertiesBean.properties['testReportParsing.reportType'] == 'nunit' ? true : false}"/>--%>

<l:settingsGroup title="Test Reports Settings">

  <tr id="testReportParsing.reportType.container">
    <th><label for="testReportParsing.reportType">Import data from:</label></th>
    <td>
      <c:set var="onchange">
        var selectedValue = this.options[this.selectedIndex].value;
        if (selectedValue == '') {
        $('testReportParsing.reportDirs.container').style.display = 'none';
        $('testReportParsing.verboseOutput.container').style.display = 'none';
        } else {
        $('testReportParsing.reportDirs.container').style.display = '';
        $('testReportParsing.verboseOutput.container').style.display = '';
        }
        $('testReportParsing.reportDirs.container').disabled = (selectedValue == '');
        $('testReportParsing.verboseOutput.container').disabled = (selectedValue == '');
      </c:set>
      <props:selectProperty name="testReportParsing.reportType"
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

  <tr id="testReportParsing.reportDirs.container" style="${displayJUnitSettings ? '' : 'display: none;'}">
    <th><label for="testReportParsing.reportDirs">Test report directories:</label></th>
    <td>
      <props:textProperty name="testReportParsing.reportDirs" className="longField"/>
          <span class="smallNote">
          ";" separated paths to directories where reports are expected to appear.
              Specified paths can be absolute or relative to the working directory.
          </span>
    </td>
  </tr>

  <tr id="testReportParsing.verboseOutput.container" style="${displayJUnitSettings ? '' : 'display: none;'}">
    <th><label for="testReportParsing.verboseOutput">Verbose output:</label></th>
    <td>
      <props:checkboxProperty name="testReportParsing.verboseOutput"/>
    </td>
  </tr>

  <%--<tr id="testReportParsing.verboseOutput.container" style="${displayFindBugsSettings ? '' : 'display: none;'}">--%>
  <%--<th><label for="testReportParsing.max.errors">Maximum error limit:</label></th>--%>
  <%--<td><props:textProperty name="testReportParsing.max.errors" style="width:6em;" maxlength="12"/>--%>
  <%--<!--<span class="error" id="error_fxcop.fail.error.limit"></span>-->--%>
  <%--<span class="smallNote">Fail the build if the specified number of errors is exceeded.</span>--%>
  <%--</td>--%>
  <%--</tr>--%>


  <tr id="testReportParsing.verboseOutput.container" style="${displayFindBugsSettings ? '' : 'display: none;'}">
    <th class="noBorder"><label for="testReportParsing.max.warnings">Warnings limit:</label></th>
    <td class="noBorder"><props:textProperty name="testReportParsing.max.warnings" style="width:6em;" maxlength="12"/>
      <!--<span class="error" id="error_fxcop.fail.warning.limit"></span>-->
      <span class="smallNote">Fail the build if the specified number of warnings is exceeded. Leave blank if there is no limit.</span>
    </td>
  </tr>
</l:settingsGroup>