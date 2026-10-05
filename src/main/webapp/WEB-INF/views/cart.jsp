<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Varukorg" />
<%@ include file="header.jspf" %>
<c:choose>
    <c:when test="${empty cartView.lines}">
        <p>Din varukorg är tom.</p>
        <a href="<c:url value='/products' />">Visa varor</a>
    </c:when>
    <c:otherwise>
        <p>Ändra antalet och välj Uppdatera. Antalet 0 tar bort varan.</p>
        <div class="table-scroll">
            <table>
                <thead><tr><th>Vara</th><th>Pris per styck</th><th>I lager</th><th>Antal</th><th>Summa</th></tr></thead>
                <tbody>
                <c:forEach var="line" items="${cartView.lines}">
                    <tr>
                        <td><c:out value="${line.product.name}" /></td>
                        <td class="number"><fmt:formatNumber value="${line.product.price}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                        <td><c:out value="${line.product.stock}" /> st</td>
                        <td>
                            <form class="inline" method="post" action="<c:url value='/cart/update' />">
                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                <input type="hidden" name="productId" value="<c:out value='${line.product.id}' />">
                                <label class="sr-only" for="quantity-${line.product.id}">Antal av <c:out value="${line.product.name}" /></label>
                                <input class="quantity" id="quantity-${line.product.id}" name="quantity" type="number" min="0" max="999" required value="<c:out value='${line.quantity}' />">
                                <button class="secondary" type="submit">Uppdatera</button>
                            </form>
                        </td>
                        <td class="number"><fmt:formatNumber value="${line.total}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <p class="total">Totalt: <fmt:formatNumber value="${cartView.total}" minFractionDigits="2" maxFractionDigits="2" /> kr</p>
        <form method="post" action="<c:url value='/checkout' />">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <button type="submit">Skicka order</button>
        </form>
    </c:otherwise>
</c:choose>
<%@ include file="footer.jspf" %>
