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
package de.rwth.idsg.steve.web.controller;

import de.rwth.idsg.steve.repository.ConnectorRepository;
import de.rwth.idsg.steve.repository.dto.Connector;
import de.rwth.idsg.steve.utils.mapper.ConnectorDetailsMapper;
import de.rwth.idsg.steve.web.dto.ConnectorForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Daniel Christen
 * @since 24.06.2021
 *
 */
@Controller
@RequestMapping(value = "/manager/connectors")
public class ConnectorsController {

    @Autowired protected ConnectorRepository connectorRepository;

    //protected static final String PARAMS = "params";

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    protected static final String DETAILS_PATH = "/details/{connectorPk}";
    protected static final String UPDATE_PATH = "/update";

    // -------------------------------------------------------------------------
    // HTTP methods
    // -------------------------------------------------------------------------

    @RequestMapping(method = RequestMethod.GET)
    public String getOverview(Model model) {
        initList(model);
        return "data-man/connectors";
    }

    private void initList(Model model) {
        model.addAttribute("cList", connectorRepository.getOverview());
    }

    @RequestMapping(value = DETAILS_PATH, method = RequestMethod.GET)
    public String getDetails(@PathVariable("connectorPk") int connectorPk, Model model) {
        Connector.Details c = connectorRepository.getDetails(connectorPk);
        ConnectorForm form = ConnectorDetailsMapper.mapToForm(c);

        model.addAttribute("connectorForm", form);
        model.addAttribute("c", c);

        return "data-man/connectorDetails";
    }


    @RequestMapping(params = "update", value = UPDATE_PATH, method = RequestMethod.POST)
    public String update(@Valid @ModelAttribute("connectorForm") ConnectorForm connectorForm,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "data-man/connectorDetails";
        }

        connectorRepository.updateConnector(connectorForm);
        return toOverview();
    }

    
    // -------------------------------------------------------------------------
    // Back to Overview
    // -------------------------------------------------------------------------

    protected String toOverview() {
        return "redirect:/manager/connectors";
    }

    @RequestMapping(params = "backToOverview", value = UPDATE_PATH, method = RequestMethod.POST)
    public String updateBackToOverview() {
        return toOverview();
    }
}
