<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="reportTypeForm" scope="request" class="jetbrains.buildServer.xmlReportPlugin.ReportTypeForm"/>

<c:set var="displayJUnitSettings"
       value="${not empty propertiesBean.properties['xmlReportParsing.reportType'] ? true : false}"/>
<c:set var="displayFindBugsSettings"
       value="${propertiesBean.properties['xmlReportParsing.reportType'] == 'findBugs' ? true : false}"/>
<%--<c:set var="displayNUnitSettings" value="${propertiesBean.properties['xmlReportParsing.reportType'] == 'nunit' ? true : false}"/>--%>

<l:settingsGroup title="XML Report Processing">

  <tr class="noBorder" id="xmlReportParsing.reportType.container">
    <th><label for="xmlReportParsing.reportType">Import data from:</label></th>
    <td>
      <c:set var="onchange">
        var selectedValue = this.options[this.selectedIndex].value;
        if (selectedValue == '') {
        $('xmlReportParsing.reportDirs.container').style.display = 'none';
        $('xmlReportParsing.verboseOutput.container').style.display = 'none';
        } else {
        $('xmlReportParsing.reportDirs.container').style.display = '';
        $('xmlReportParsing.verboseOutput.container').style.display = '';
        }
        $('xmlReportParsing.reportDirs.container').disabled = (selectedValue == '');
        $('xmlReportParsing.verboseOutput.container').disabled = (selectedValue == '');
        if (selectedValue == 'findBugs') {
        $('xmlReportParsing.max.errors.container').style.display = '';
        $('xmlReportParsing.max.warnings.container').style.display = '';
        } else {
        $('xmlReportParsing.max.errors.container').style.display = 'none';
        $('xmlReportParsing.max.warnings.container').style.display = 'none';
        }
        $('xmlReportParsing.max.errors.container').disabled = (selectedValue == 'findbugs');
        $('xmlReportParsing.max.warnings.container').disabled = (selectedValue == 'findbugs');
      </c:set>
      <props:selectProperty name="xmlReportParsing.reportType"
                            onchange="${onchange}">
        <c:set var="selected" value="false"/>
        <c:if test="${empty propertiesBean.properties['xmlReportParsing.reportType']}">
          <c:set var="selected" value="true"/>
        </c:if>
        <props:option value="" selected="${selected}">&lt;Do not import&gt;</props:option>
        <c:forEach var="reportType" items="${reportTypeForm.availableReportTypes}">
          <c:set var="selected" value="false"/>
          <c:if test="${reportType.type == propertiesBean.properties['xmlReportParsing.reportType']}">
            <c:set var="selected" value="true"/>
          </c:if>
          <props:option value="${reportType.type}"
                        selected="${selected}"><c:out value="${reportType.displayName}"/></props:option>
        </c:forEach>
      </props:selectProperty>
      <span class="smallNote">Choose report format.</span>
    </td>
  </tr>

  <tr class="noBorder" id="xmlReportParsing.reportDirs.container"
      style="${displayJUnitSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.reportDirs">Report paths:</label></th>
      <%--<td>--%>
      <%--<props:textProperty name="xmlReportParsing.reportDirs" className="longField"/>--%>
      <%--<span class="smallNote">--%>
      <%--";" separated paths to directories where reports are expected to appear.--%>
      <%--Specified paths can be absolute or relative to the working directory.--%>
      <%--</span>--%>
      <%--</td>--%>
    <td>
      <props:multilineProperty name="xmlReportParsing.reportDirs" expanded="true" rows="5" cols="50"
                               linkTitle="Type report directories"/>
        <span class="smallNote">
          New line or comma separated paths to reports. Specified paths can be absolute or relative to the working directory.
      <!--Support ant-style wildcards like dir/**/*.zip and target directories like *.zip => winFiles,unix/distro.tgz => linuxFiles, where winFiles and linuxFiles are target directories.-->
        </span>
    </td>
  </tr>

  <tr class="noBorder" id="xmlReportParsing.verboseOutput.container"
      style="${displayJUnitSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.verboseOutput">Verbose output:</label></th>
    <td>
      <props:checkboxProperty name="xmlReportParsing.verboseOutput"/>
    </td>
  </tr>

  <tr class="noBorder" id="xmlReportParsing.max.errors.container"
      style="${displayFindBugsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.max.errors">Maximum error limit:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.errors" style="width:6em;" maxlength="12"/>
      <span class="smallNote">Fail the build if the specified number of errors is exceeded.</span>
    </td>
  </tr>


  <tr class="noBorder" id="xmlReportParsing.max.warnings.container"
      style="${displayFindBugsSettings ? '' : 'display: none;'}">
    <th><label for="xmlReportParsing.max.warnings">Warnings limit:</label></th>
    <td><props:textProperty name="xmlReportParsing.max.warnings" style="width:6em;" maxlength="12"/>
      <span class="smallNote">Fail the build if the specified number of warnings is exceeded. Leave blank if there is no limit.</span>
    </td>
  </tr>
</l:settingsGroup>