package com.travelplanner.requirements.domain;

/** Dados de um país vistos pelas regras e pelas telas. */
public record CountryInfo(
    String code,
    String namePt,
    String flagEmoji,
    String plugTypes,
    String voltage,
    String currency,
    boolean schengen,
    boolean brIdCardAccepted) {}
