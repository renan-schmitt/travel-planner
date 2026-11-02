package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.domain.TripShareLink;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Link de partilha ativo. O frontend monta a URL {@code /join/{token}}. */
public record ShareLinkResponse(
    @NotNull Long id, @NotNull String token, @NotNull TripRole role, @NotNull Instant createdAt) {

  static ShareLinkResponse from(TripShareLink link) {
    return new ShareLinkResponse(
        link.getId(), link.getToken(), link.getRole(), link.getCreatedAt());
  }
}
