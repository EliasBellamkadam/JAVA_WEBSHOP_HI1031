<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Hantera användare" />
<%@ include file="header.jspf" %>
<h2><c:choose><c:when test="${empty editUser}">Ny användare</c:when><c:otherwise>Redigera användare</c:otherwise></c:choose></h2>
<form class="editor" method="post" action="<c:url value='/admin/users/save' />">
    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
    <input type="hidden" name="id" value="<c:out value='${editUser.id}' />">
    <label for="username">Användarnamn</label>
    <input id="username" name="username" maxlength="50" autocomplete="off" required value="<c:out value='${editUser.username}' />">
    <label for="password">Lösenord<c:if test="${not empty editUser}"> (lämna tomt för att behålla)</c:if></label>
    <input id="password" name="password" type="password" minlength="8" maxlength="128" autocomplete="new-password" <c:if test="${empty editUser}">required</c:if>>
    <label for="role">Behörighet</label>
    <select id="role" name="role" required>
        <c:forEach var="role" items="${roles}">
            <option value="<c:out value='${role}' />" <c:if test="${editUser.role == role}">selected</c:if>><c:out value="${role.label}" /></option>
        </c:forEach>
    </select>
    <label class="checkbox"><input name="active" type="checkbox" value="true" <c:if test="${empty editUser or editUser.active}">checked</c:if>> Aktiv användare</label>
    <div class="inline"><button type="submit">Spara användare</button><c:if test="${not empty editUser}"><a href="<c:url value='/admin/users' />">Avbryt</a></c:if></div>
</form>
<h2>Användare</h2>
<div class="table-scroll">
    <table>
        <thead><tr><th>Användarnamn</th><th>Behörighet</th><th>Status</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="user" items="${users}">
            <c:url var="editUrl" value="/admin/users"><c:param name="id" value="${user.id}" /></c:url>
            <tr>
                <td><c:out value="${user.username}" /></td>
                <td><c:out value="${user.role.label}" /></td>
                <td><c:choose><c:when test="${user.active}">Aktiv</c:when><c:otherwise>Inaktiverad</c:otherwise></c:choose></td>
                <td><a href="<c:out value='${editUrl}' />">Redigera</a></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="footer.jspf" %>
