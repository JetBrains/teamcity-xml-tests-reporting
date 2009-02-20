<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Test report monitoring:
  <c:choose>
    <c:when test="${empty propertiesBean.properties['xmlReportParsing.reportType']}">
      <strong>disabled</strong>
    </c:when>
    <c:otherwise>
      <strong>enabled</strong>
    </c:otherwise>
  </c:choose>
</div>

<c:if test="${not empty propertiesBean.properties['xmlReportParsing.reportType']}">
  <div class="parameter">
    Test report type: <props:displayValue name="xmlReportParsing.reportType"
                                          emptyValue="none specified"/>
  </div>

  <div class="parameter">
    Test report directories: <props:displayValue name="xmlReportParsing.reportDirs"
                                                 emptyValue="none specified"/>
  </div>

  <div class="parameter">
    Verbose output:
    <c:choose>
      <c:when test="${propertiesBean.properties['xmlReportParsing.verboseOutput']}">
        <strong>enabled</strong>
      </c:when>
      <c:otherwise>
        <strong>disabled</strong>
      </c:otherwise>
    </c:choose>
  </div>

  <c:if test="${propertiesBean.properties['xmlReportParsing.reportType'] == 'findBugs'}">
    <div class="parameter">
      Maximum error limit:
      <c:choose>
        <c:when test="${not empty propertiesBean.properties['xmlReportParsing.max.errors']}">
          <props:displayValue name="xmlReportParsing.max.errors"
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
        <c:when test="${not empty propertiesBean.properties['xmlReportParsing.max.warnings']}">
          <props:displayValue name="xmlReportParsing.max.warnings"
                              emptyValue="none specified"/>
        </c:when>
        <c:otherwise>
          <strong>no limit</strong>
        </c:otherwise>
      </c:choose>
    </div>
  </c:if>
</c:if>