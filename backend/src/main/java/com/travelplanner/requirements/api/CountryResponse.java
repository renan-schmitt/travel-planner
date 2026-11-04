package com.travelplanner.requirements.api;

import com.travelplanner.requirements.domain.CountryInfo;
import jakarta.validation.constraints.NotNull;

/** País da base ({@code plugTypes}: tipos IEC separados por vírgula, ex.: "C,F"). */
public record CountryResponse(
    @NotNull String code,
    @NotNull String name,
    @NotNull String flag,
    String plugTypes,
    String voltage,
    String currency,
    @NotNull Boolean schengen,
    @NotNull Boolean brIdCardAccepted) {

  static CountryResponse from(CountryInfo c) {
    return c == null
        ? null
        : new CountryResponse(
            c.code(),
            c.namePt(),
            c.flagEmoji(),
            c.plugTypes(),
            c.voltage(),
            c.currency(),
            c.schengen(),
            c.brIdCardAccepted());
  }
}
