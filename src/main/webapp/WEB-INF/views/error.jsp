<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Åtgärden kunde inte genomföras" />
<%@ include file="header.jspf" %>
<p class="error" role="alert"><c:choose><c:when test="${not empty error}"><c:out value="${error}" /></c:when><c:when test="${requestScope['jakarta.servlet.error.status_code'] == 404}">Sidan finns inte.</c:when><c:otherwise>Ett oväntat fel inträffade. Försök igen.</c:otherwise></c:choose></p>
<c:choose><c:when test="${empty currentUser}"><a href="<c:url value='/login' />">Till inloggningen</a></c:when><c:otherwise><a href="<c:url value='/products' />">Till varorna</a></c:otherwise></c:choose>
<%@ include file="footer.jspf" %>
