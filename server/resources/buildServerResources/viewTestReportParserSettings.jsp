<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Test report monitoring:
  <c:choose>
    <c:when test="${propertiesBean.properties['testReportParsing.reportType'] == ''}">
      <strong>disabled</strong>
    </c:when>
    <c:otherwise>
      <strong>enabled</strong>
    </c:otherwise>
  </c:choose>
</div>

<c:if test="${propertiesBean.properties['testReportParsing.reportType'] == 'junit' or
              propertiesBean.properties['testReportParsing.reportType'] == 'nunit'}">
  <div class="parameter">
    Test report type: <props:displayValue name="testReportParsing.reportType"
                                          emptyValue="none specified"/>
  </div>

  <div class="parameter">
    Test report directories: <props:displayValue name="testReportParsing.reportDirs"
                                                 emptyValue="none specified"/>
  </div>

  <div class="parameter">
    Verbose output:
    <c:choose>
      <c:when test="${propertiesBean.properties['testReportParsing.verboseOutput']}">
        <strong>enabled</strong>
      </c:when>
      <c:otherwise>
        <strong>disabled</strong>
      </c:otherwise>
    </c:choose>
  </div>
</c:if>