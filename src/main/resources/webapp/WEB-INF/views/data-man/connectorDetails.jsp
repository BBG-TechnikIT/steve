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
<spring:hasBindErrors name="cconnectorForm">
    <div class="error">
        Error while trying to update a charge point:
        <ul>
            <c:forEach var="error" items="${errors.allErrors}">
                <li>${error.defaultMessage}</li>
            </c:forEach>
        </ul>
    </div>
</spring:hasBindErrors>
<div class="content"><div>
    <section><span>
        Connector Details
        <a class="tooltip" href="#"><img src="${ctxPath}/static/images/info.png" style="vertical-align:middle">
            <span>Einige Felder koennen nicht bearbeitet werden</span>
        </a>
    </span></section>

    <table class="userInput">
        <thead><tr><th>Related Data Pages</th><th></th></thead>
        <tbody>
        <tr>
            <td>Transactions:</td>
            <td>
                <a href="${ctxPath}/manager/transactions/query?connectorId=${connectorForm.connectorId}&amp;chargeBoxId=${connectorForm.chargeBoxId}&amp;type=ACTIVE">ACTIVE</a>
                    /
                <a href="${ctxPath}/manager/transactions/query?connectorId=${connectorForm.connectorId}&amp;chargeBoxId=${connectorForm.chargeBoxId}&amp;type=ALL">ALL</a>
            </td>
        </tr>
        <tr>
            <td>Reservations:</td>
            <td>
                <a href="${ctxPath}/manager/reservations/query?connectorId=${connector.connectorPk}&amp;periodType=ACTIVE">ACTIVE</a>
            </td>
        </tr>
        </tbody>
    </table>

    <form:form action="${ctxPath}/manager/connectors/update" modelAttribute="connectorForm">

        <form:hidden path="connectorPk" readonly="true"/>
        <table class="userInput">
            <thead><tr><th>Infos</th><th></th></thead>
            <tbody>
                <tr>
                    <td>Connector ID:</td>
                    <td>
                        <form:input path="connectorId" readonly="true" />
                        <a class="tooltip" href="#"><img src="${ctxPath}/static/images/info.png" style="vertical-align:middle">
                            <span>This field is set when adding a charge point, and cannot be changed later</span>
                        </a>
                    </td>
                </tr>
                <tr>
                    <td>Connector Interne ID:</td>
                    <td>
                        <form:input path="connectorPk" readonly="true" />
                        <a class="tooltip" href="#"><img src="${ctxPath}/static/images/info.png" style="vertical-align:middle">
                            <span>This field is set when adding a charge point, and cannot be changed later</span>
                        </a>
                    </td>
                </tr>
                <tr>
                    <td>ChargeBox ID:</td>
                    <td>
                        <form:input path="chargeBoxId" readonly="true" />
                        <a class="tooltip" href="#"><img src="${ctxPath}/static/images/info.png" style="vertical-align:middle">
                            <span>This field is set when adding a charge point, and cannot be changed later</span>
                        </a>
                    </td>
                </tr>
                <tr><td>Connector Description:</td><td><form:input path="connectorDescription"/></td></tr>
            <tr><td></td>
            <td id="add_space">
                <input type="submit" name="update" value="Update">
                <input type="submit" name="backToOverview" value="Back to Overview">
            </td></tr>
            </tbody>
        </table>
    </form:form>
</div>
<%@ include file="../00-footer.jsp" %>
