<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<l:settingsGroup title="Ant JUnit Reports Settings">

  <tr>
    <th class="noBorder"><label>Ant JUnit reports:</label></th>
    <td class="noBorder">
      <c:set var="onclick">
        $('testReportParsing.reportDirs').disabled = !this.checked;
        $('testReportParsing.verboseOutput').disabled = !this.checked;
      </c:set>
      <props:checkboxProperty name="testReportParsing.enabled" onclick="${onclick}"/>
      <label for="testReportParsing.enabled">Enable Ant JUnit report monitoring</label>
      <span class="smallNote">Tests will be logged as soon as report files appear.</span>
      <br/>
    </td>
  </tr>

  <tr id="testReportParsing.reportDirs.container">
    <th><label for="testReportParsing.reportDirs">Test report directories:</label></th>
    <td>
      <props:textProperty name="testReportParsing.reportDirs" className="longField"
                          disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"/>
        <span class="smallNote">
        ";" separated paths to directories where reports are expected to appear.
            Specified paths can be absolute or relative to the working directory.
        </span>
      <br/>
    </td>
  </tr>

  <tr>
    <th class="noBorder"><label>Verbose output:</label></th>
    <td class="noBorder">
      <props:checkboxProperty name="testReportParsing.verboseOutput"
                              disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"/>
      <br/>
    </td>
  </tr>

</l:settingsGroup>