package com.travelplanner.requirements.domain;

/**
 * Publicado quando um requisito do país muda (criar, editar, publicar, despublicar, excluir). O
 * motor de lacunas recalcula as viagens não encerradas com parada no país.
 */
public record RequirementsChangedEvent(String countryCode) {}
