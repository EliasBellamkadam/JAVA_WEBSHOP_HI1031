<%@ include file="prelude.jspf" %>
<c:set var="pageTitle" value="Ordrar" />
<%@ include file="header.jspf" %>
<c:if test="${empty orders}"><p>Det finns inga ordrar ännu.</p></c:if>
<c:forEach var="order" items="${orders}">
    <section class="order" aria-labelledby="order-${order.id}">
        <h2 id="order-${order.id}">Order <c:out value="${order.id}" /></h2>
        <p>Kund: <c:out value="${order.username}" /> · Skapad: <c:out value="${order.createdAt}" /></p>
        <p>Status: <c:choose><c:when test="${order.status == 'PACKED'}">Packad</c:when><c:otherwise>Inskickad</c:otherwise></c:choose></p>
        <div class="table-scroll">
            <table>
                <thead><tr><th>Vara</th><th>Antal</th><th>Pris per styck</th><th>Summa</th></tr></thead>
                <tbody>
                <c:forEach var="line" items="${order.lines}">
                    <tr>
                        <td><c:out value="${line.productName}" /></td>
                        <td><c:out value="${line.quantity}" /></td>
                        <td class="number"><fmt:formatNumber value="${line.unitPrice}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                        <td class="number"><fmt:formatNumber value="${line.total}" minFractionDigits="2" maxFractionDigits="2" /> kr</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <c:if test="${order.status == 'NEW'}">
            <form method="post" action="<c:url value='/warehouse/orders/pack' />">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                <input type="hidden" name="id" value="<c:out value='${order.id}' />">
                <button type="submit">Markera som packad</button>
            </form>
        </c:if>
    </section>
</c:forEach>
<%@ include file="footer.jspf" %>
