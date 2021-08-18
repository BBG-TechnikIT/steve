package de.rwth.idsg.steve.repository.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * @author Daniel Christen
 * @since 23.07.2021
 */
@Getter
@Builder
public class SmsSettings{
    private final boolean enabled;
    private final String host, standbyHost, username, protocol, password;
}