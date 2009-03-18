<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="reportTypeForm" scope="request" class="jetbrains.buildServer.xmlReportPlugin.ReportTypeForm"/>

<c:set var="displayJUnitSettings"
       value="${not empty propertiesBean.properties['xmlReportParsing.reportType'] ? true : false}"/>
<c:set var="displayInspectionsSettings"
       value="${propertiesBean.properties['xmlReportParsing.reportType'] == 'findBugs' ||
                propertiesBean.properties['xmlReportParsing.reportType'] == 'pmd' ? true : false}"/>

<l:settingsGroup title="XML Report Processing">
  <c:if test="${buildForm.buildRunnerBean.runnerType != 'simpleRunner'}">
  <tr class="noBorder" id="xmlReportParsing.reportType.container">
    <td colspan="2">
    Choose a report type to import. You only need to import tests reports if automatic tests reporting fails to detect your tests.
    </td>
  </tr>
  </c:if>
  <tr class="noBorder" id="xmlReportParsing.reportType.container">
    <th><label for="xmlReportParsing.reportType">Import data from:</label></th>
    <td>
      <c:set var="onchange">
        var selectedValue = this.options[this.selectedIndex].value;
        if (selectedValue == '') {
        BS.Util.hide($('xmlReportParsing.reportDirs.container'));
        BS.Util.hide($('xmlReportParsing.verboseOutput.container'));
        } else {
        BS.Util.show($('xmlReportParsing.reportDirs.container'));
        BS.Util.show($('xmlReportParsing.verboseOutput.container'));
        }
        $('xmlReportParsing.reportDirs.container').disabled = (selectedValue == '');
        $('xmlReportParsing.verboseOutput.container').disabled = (selectedValue == '');
        var isInspection = (selectedValue == 'findBugs' || selectedValue == 'pmd');
        if (isInspection) {
        BS.Util.show($('xmlReportParsing.max.errors.container'));
        BS.Util.show($('xmlReportParsing.max.warnings.container'));
        } else {
        BS.Util.hide($('xmlReportParsing.max.errors.container'));
        BS.Util.hide($('xmlReportParsing.max.warnings.container'));
        }
        $('xmlReportParsing.max.errors.container').disabled = (isInspection);
        $('xmlReportParsing.max.warnings.container').disabled = (isInspection);

        BS.MultilineProperties.updateVisible();
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
    <td>
      <props:multilineProperty name="xmlReportParsing.reportDirs" expanded="true" rows="5" cols="50"
                               linkTitle="Type report directories"/>
        <span class="smallNote">
          New line or comma separated paths to reports. Specified paths can be absolute or relative to the checkout directory.
          Support ant-style wildcards like <b>dir/**/*.zip</b>. To ensure monitoring swiftness specify more concrete paths.
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
</l:settingsGroup>