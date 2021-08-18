<%--

    SteVe - SteckdosenVerwaltung - https://github.com/RWTH-i5-IDSG/steve
    Copyright (C) 2013-2021 RWTH Aachen University - Information Systems - Intelligent Distributed Systems Group (IDSG).
    All Rights Reserved.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

    Daniel Christen 24.06.2021

--%>
<%@ include file="../00-header.jsp" %>
<script type="text/javascript">
    $(document).ready(function() {
        <%@ include file="../snippets/sortable.js" %>
        $("#unknown").click(function () {
            $("#unknownTable, #overview").slideToggle(250);
        });
    });
</script>
<div class="content">

    <div id="overview">
        <table class="res action">
            <thead>
            <tr>
                <th data-sort="string">Connector Internal ID</th>
                <th data-sort="string">Connector ID</th>
                <th data-sort="string">Connector Description</th>
                <th data-sort="string">ChargeBox ID</th>
                <th data-sort="string">ChargeBox Description</th>
                <th>Aktion</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${cList}" var="c">
                <tr><td><a href="${ctxPath}/manager/connectors/details/${c.connectorPk}">${c.connectorPk}</a></td>
                    <td>${c.connectorId}</td>
                    <td>${c.connectorDescription}</td>
                    <td>${c.chargeBoxId}</td>
                    <td>${c.chargeBoxDescription}</td>
                    <td>
                        <form:form action="${ctxPath}/manager/connectors/details/${c.connectorPk}" METHOD="GET">
                            <input type="submit" class="blueSubmit" value="Edit">
                        </form:form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div></div>
<%@ include file="../00-footer.jsp" %>