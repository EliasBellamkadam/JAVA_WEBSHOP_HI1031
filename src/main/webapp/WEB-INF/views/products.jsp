<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Varor" />
<%@ include file="header.jspf" %>
<c:choose>
    <c:when test="${empty products}"><p>Det finns inga varor ännu.</p></c:when>
    <c:otherwise>
        <div class="table-scroll">
            <table>
                <thead><tr><th>Vara</th><th>Kategori</th><th>Pris</th><th>Lager</th><th>Lägg i varukorg</th></tr></thead>
                <tbody>
                <c:forEach var="product" items="${products}">
                    <tr>
                        <td><c:out value="${product.name}" /></td>
                        <td><c:out value="${product.categoryName}" /></td>
                        <td class="number"><fmt:formatNumber value="${product.price}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                        <td><c:choose><c:when test="${product.stock > 0}"><c:out value="${product.stock}" /> st</c:when><c:otherwise>Slut i lager</c:otherwise></c:choose></td>
                        <td>
                            <c:if test="${product.stock > 0}">
                                <form class="inline" method="post" action="<c:url value='/cart/add' />">
                                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                    <input type="hidden" name="productId" value="<c:out value='${product.id}' />">
                                    <label class="sr-only" for="quantity-${product.id}">Antal av <c:out value="${product.name}" /></label>
                                    <input class="quantity" id="quantity-${product.id}" name="quantity" type="number" min="1" max="999" value="1" required>
                                    <button type="submit">Lägg till</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="footer.jspf" %>
