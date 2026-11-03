package com.travelplanner.suggestion.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do motor de lacunas ({@code app.suggestions.*}).
 *
 * @param reconcileOnStartup recalcula as viagens não encerradas quando a aplicação sobe
 */
@ConfigurationProperties("app.suggestions")
public record SuggestionProperties(@DefaultValue("true") boolean reconcileOnStartup) {}
