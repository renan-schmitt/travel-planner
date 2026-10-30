package com.travelplanner.auth.api;

import com.travelplanner.auth.domain.AppUser;
import jakarta.validation.constraints.NotNull;

/** Usuário logado. {@code homeCity} é nulo enquanto não for preenchida no perfil. */
public record MeResponse(
    @NotNull Long id,
    @NotNull String email,
    String name,
    String pictureUrl,
    @NotNull String nationality,
    HomeCityDto homeCity,
    @NotNull Boolean isAdmin,
    @NotNull Long version) {

  static MeResponse from(AppUser user, boolean isAdmin) {
    return new MeResponse(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getPictureUrl(),
        user.getNationality(),
        HomeCityDto.from(user.getHomeCity()),
        isAdmin,
        user.getVersion());
  }
}
