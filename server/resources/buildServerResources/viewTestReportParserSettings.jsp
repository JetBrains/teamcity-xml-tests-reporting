<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Test report monitoring:
  <c:choose>
    <c:when test="${empty propertiesBean.properties['testReportParsing.reportType']}">
      <strong>disabled</strong>
    </c:when>
    <c:otherwise>
      <strong>enabled</strong>
    </c:otherwise>
  </c:choose>
</div>

<c:if test="${not empty propertiesBean.properties['testReportParsing.reportType']}">
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

  <c:if test="${propertiesBean.properties['testReportParsing.reportType'] == 'findBugs'}">
    <div class="parameter">
      Maximum error limit:
      <c:choose>
        <c:when test="${not empty propertiesBean.properties['testReportParsing.max.errors']}">
          <props:displayValue name="testReportParsing.max.errors"
                              emptyValue="none specified"/>
        </c:when>
        <c:otherwise>
          <strong>no limit</strong>
        </c:otherwise>
      </c:choose>
    </div>
    <div class="parameter">
      Warnings limit:
      <c:choose>
        <c:when test="${not empty propertiesBean.properties['testReportParsing.max.warnings']}">
          <props:displayValue name="testReportParsing.max.warnings"
                              emptyValue="none specified"/>
        </c:when>
        <c:otherwise>
          <strong>no limit</strong>
        </c:otherwise>
      </c:choose>
    </div>
  </c:if>
</c:if>