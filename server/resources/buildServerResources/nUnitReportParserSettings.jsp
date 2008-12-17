<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>


<l:settingsGroup title="NUnit Test Reports Settings">
  <tr id="testReportParsing.reportDirs.container">
    <th><label for="testReportParsing.reportDirs">Test report directories:</label></th>
    <td>
      <props:textProperty name="testReportParsing.reportDirs" className="longField"
                          disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"/>
        <span class="smallNote">
        ";" separated paths to directories where reports are expected to appear.
            Specified paths can be absolute or relative to the working directory.
        </span>
    </td>
  </tr>

  <tr id="testReportParsing.verboseOutput.container">
    <th><label for="testReportParsing.verboseOutput">Verbose output:</label></th>
    <td>
      <props:checkboxProperty name="testReportParsing.verboseOutput"
                              disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"/>
    </td>
  </tr>
</l:settingsGroup>