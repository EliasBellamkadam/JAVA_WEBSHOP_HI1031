<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Hantera varor" />
<%@ include file="header.jspf" %>
<h2><c:choose><c:when test="${empty editProduct}">Ny vara</c:when><c:otherwise>Redigera vara</c:otherwise></c:choose></h2>
<c:choose>
    <c:when test="${empty categories}"><p>Skapa en <a href="<c:url value='/admin/categories' />">kategori</a> innan du lägger till en vara.</p></c:when>
    <c:otherwise>
        <form class="editor" method="post" action="<c:url value='/admin/products/save' />">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <input type="hidden" name="id" value="<c:out value='${editProduct.id}' />">
            <label for="name">Namn</label>
            <input id="name" name="name" maxlength="100" required value="<c:out value='${editProduct.name}' />">
            <label for="categoryId">Kategori</label>
            <select id="categoryId" name="categoryId" required>
                <c:forEach var="category" items="${categories}">
                    <option value="<c:out value='${category.id}' />" <c:if test="${editProduct.categoryId == category.id}">selected</c:if>><c:out value="${category.name}" /></option>
                </c:forEach>
            </select>
            <label for="price">Pris (kr)</label>
            <input id="price" name="price" type="number" min="0" max="9999999999.99" step="0.01" required value="<c:out value='${editProduct.price}' />">
            <label for="stock">Lagersaldo</label>
            <input id="stock" name="stock" type="number" min="0" max="1000000" required value="<c:out value='${empty editProduct ? 0 : editProduct.stock}' />">
            <div class="inline"><button type="submit">Spara vara</button><c:if test="${not empty editProduct}"><a href="<c:url value='/admin/products' />">Avbryt</a></c:if></div>
        </form>
    </c:otherwise>
</c:choose>
<h2>Varor</h2>
<div class="table-scroll">
    <table>
        <thead><tr><th>Vara</th><th>Kategori</th><th>Pris</th><th>Lagersaldo</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="product" items="${products}">
            <c:url var="editUrl" value="/admin/products"><c:param name="id" value="${product.id}" /></c:url>
            <tr>
                <td><c:out value="${product.name}" /></td>
                <td><c:out value="${product.categoryName}" /></td>
                <td class="number"><fmt:formatNumber value="${product.price}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                <td><c:out value="${product.stock}" /> st</td>
                <td><a href="<c:out value='${editUrl}' />">Redigera</a></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="footer.jspf" %>
