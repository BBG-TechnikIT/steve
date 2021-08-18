package de.rwth.idsg.steve.utils.mapper;

import de.rwth.idsg.steve.repository.dto.Connector;
import de.rwth.idsg.steve.web.dto.ConnectorForm;
import jooq.steve.db.tables.records.ConnectorRecord;

/**
 * @author Daniel Christen
 * @since 25.06.2021
 */
public class ConnectorDetailsMapper {

    public static ConnectorForm mapToForm(Connector.Details c) {
        ConnectorRecord connector = c.getConnector();

        ConnectorForm form = new ConnectorForm();

        form.setConnectorPk(connector.getConnectorPk());
        form.setConnectorId(connector.getConnectorId());
        form.setConnectorDescription(connector.getConnectorDescription());
        form.setChargeBoxId(connector.getChargeBoxId());

        return form;
    }

}
