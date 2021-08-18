/*
 * SteVe - SteckdosenVerwaltung - https://github.com/RWTH-i5-IDSG/steve
 * Copyright (C) 2013-2021 RWTH Aachen University - Information Systems - Intelligent Distributed Systems Group (IDSG).
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.repository.impl;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.ConnectorRepository;
import de.rwth.idsg.steve.repository.dto.Connector;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import de.rwth.idsg.steve.web.dto.ConnectorForm;
import de.rwth.idsg.steve.web.dto.ConnectorStatusForm;
import jooq.steve.db.tables.records.ConnectorRecord;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.rwth.idsg.steve.utils.CustomDSL.date;
import static de.rwth.idsg.steve.utils.CustomDSL.includes;
import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;

/**
 * @author Daniel Christen
 * @since 25.06.2021
 */
@Slf4j
@Repository
public class ConnectorRepositoryImpl implements ConnectorRepository {

    private final DSLContext ctx;

    @Autowired
    public ConnectorRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<Connector.Overview> getOverview() {
        return getOverviewInternal();
    }

    @SuppressWarnings("unchecked")
    private List<Connector.Overview> getOverviewInternal() {
        Field<Integer> cPK = CONNECTOR.CONNECTOR_PK.as("cPK");
        Field<Integer> cID = CONNECTOR.CONNECTOR_ID.as("cID");
        Field<String> cDescription = CONNECTOR.CONNECTOR_DESCRIPTION.as("cDescription");
        Field<String> cbID = CONNECTOR.CHARGE_BOX_ID.as("cbID");
        Field<String> cbDescription = CHARGE_BOX.DESCRIPTION.as("cbDescription");
        return ctx.select(cPK, cbID, cbDescription, cID, cDescription)
                        .from(CONNECTOR)
                        .join(CHARGE_BOX)
                            .on(CONNECTOR.CHARGE_BOX_ID.eq(CHARGE_BOX.CHARGE_BOX_ID))
                        .where(CONNECTOR.CONNECTOR_ID.notEqual(0))
                        .fetch()
                        .map(r -> Connector.Overview.builder()
                                .connectorPk(r.value1())
                                .chargeBoxId(r.value2())
                                .chargeBoxDescription(r.value3())
                                .connectorId(r.value4())
                                .connectorDescription(r.value5())
                                .build()
                        );
    }

    @Override
    public void updateConnector(ConnectorForm form){
        ctx.transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            try {
                updateConnectorInternal(ctx, form);

            } catch (DataAccessException e) {
                throw new SteveException("Failed to update the charge point with connectorId '%s'",
                        form.getConnectorId(), e);
            }
        });
    }

    @Override
    public Connector.Details getDetails(int connectorPk){
        ConnectorRecord cr = ctx.selectFrom(CONNECTOR)
                                 .where(CONNECTOR.CONNECTOR_PK.equal(connectorPk))
                                 .fetchOne();

        if (cr == null) {
            throw new SteveException("Connector not found");
        }

        return new Connector.Details(cr);
    }

    private void updateConnectorInternal(DSLContext ctx, ConnectorForm form) {
        ctx.update(CONNECTOR)
           .set(CONNECTOR.CHARGE_BOX_ID, form.getChargeBoxId())
           .set(CONNECTOR.CONNECTOR_ID, form.getConnectorId())
           .set(CONNECTOR.CONNECTOR_DESCRIPTION, form.getConnectorDescription())
           .where(CONNECTOR.CONNECTOR_PK.equal(form.getConnectorPk()))
           .execute();
    }

    @Override
    public List<Integer> getConnectorIds() {
        return ctx.selectDistinct(CONNECTOR.CONNECTOR_ID)
                  .from(CONNECTOR)
                  .where(CONNECTOR.CONNECTOR_ID.notEqual(0))
                  .fetch(CONNECTOR.CONNECTOR_ID);
    }
}
