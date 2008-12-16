<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Test report monitoring:
  <c:choose>
    <c:when test="${propertiesBean.properties['testReportParsing.enabled']}">
      <strong>enabled</strong>
    </c:when>
    <c:otherwise>
      <strong>disabled</strong>
    </c:otherwise>
  </c:choose>
</div>

<c:if test="${propertiesBean.properties['testReportParsing.enabled']}">
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