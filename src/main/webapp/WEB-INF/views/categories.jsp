<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Hantera kategorier" />
<%@ include file="header.jspf" %>
<h2><c:choose><c:when test="${empty editCategory}">Ny kategori</c:when><c:otherwise>Redigera kategori</c:otherwise></c:choose></h2>
<form class="editor" method="post" action="<c:url value='/admin/categories/save' />">
    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
    <input type="hidden" name="id" value="<c:out value='${editCategory.id}' />">
    <label for="name">Namn</label>
    <input id="name" name="name" maxlength="100" required value="<c:out value='${editCategory.name}' />">
    <div class="inline"><button type="submit">Spara kategori</button><c:if test="${not empty editCategory}"><a href="<c:url value='/admin/categories' />">Avbryt</a></c:if></div>
</form>
<h2>Kategorier</h2>
<div class="table-scroll">
    <table>
        <thead><tr><th>Kategori</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="category" items="${categories}">
            <c:url var="editUrl" value="/admin/categories"><c:param name="id" value="${category.id}" /></c:url>
            <tr><td><c:out value="${category.name}" /></td><td><a href="<c:out value='${editUrl}' />">Redigera</a></td></tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="footer.jspf" %>
