package com.travelplanner.requirements.domain;

/** Campos editáveis de um requisito (códigos de país em maiúsculas). */
public record RequirementDetails(
    String countryCode,
    String nationality,
    RequirementType type,
    String title,
    String description,
    Integer leadDays,
    String officialUrl) {}
