<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<l:settingsGroup title="Ant JUnit Reports Settings">

    <tr>
        <th class="noBorder"><label>Ant JUnit reports:</label></th>
        <td class="noBorder">
            <c:set var="onclick">
                $('testReportParsing.reoprtDirs').disabled = !this.checked;
            </c:set>
            <props:checkboxProperty name="testReportParsing.enabled" onclick="${onclick}"/><label
                for="testReportParsing.enabled">Enable Ant JUnit report monitoring</label>
            <span class="smallNote">Tests will be logged as soon as report files appear.</span>
            <br/>
                <%--<props:checkboxProperty name="coverage.include.source" disabled="${empty propertiesBean.properties['coverage.enabled']}"/>--%>
            <!--<label for="coverage.include.source">Include source files in the coverage data</label>-->
        </td>
    </tr>

    <tr id="testReportParsing.reoprtDirs.container">
        <th><label for="testReportParsing.reoprtDirs">Report directories:</label></th>
        <td>
            <props:textProperty name="testReportParsing.reoprtDirs" className="longField"
                                disabled="${empty propertiesBean.properties['testReportParsing.enabled']}"/>
      <span class="smallNote">
        ";" separated paths to directories where reports are expected to appear.
            Specified paths can be absolute or relative to the checkout directory.          
    <!--<a target="_blank" showdiscardchangesmessage="false"-->
       <!--href="http://emma.sourceforge.net/reference_single/reference.html#tool-ref.instr.cmdline">EMMA documentation</a> for details.-->
      </span>

        </td>
    </tr>

</l:settingsGroup>