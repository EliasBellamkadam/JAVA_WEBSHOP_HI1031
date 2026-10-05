<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Logga in" />
<%@ include file="header.jspf" %>
<c:if test="${not empty error}"><p class="error" role="alert"><c:out value="${error}" /></p></c:if>
<form class="editor narrow" method="post" action="<c:url value='/login' />">
    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
    <label for="username">Användarnamn</label>
    <input id="username" name="username" maxlength="50" autocomplete="username" required value="<c:out value='${username}' />">
    <label for="password">Lösenord</label>
    <input id="password" name="password" type="password" autocomplete="current-password" required>
    <button type="submit">Logga in</button>
</form>
<%@ include file="footer.jspf" %>
