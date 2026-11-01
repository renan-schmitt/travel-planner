package com.travelplanner.shared.time;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração de tempo ({@code app.time.*}).
 *
 * @param defaultZone fuso usado para saber "que dia é hoje" quando o usuário ainda não tem cidade
 *     de origem com fuso (preenchido pelo autocomplete da Fase 7)
 */
@ConfigurationProperties("app.time")
public record TimeProperties(@DefaultValue("America/Sao_Paulo") ZoneId defaultZone) {}
