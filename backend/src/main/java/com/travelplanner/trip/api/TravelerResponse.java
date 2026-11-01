package com.travelplanner.trip.api;

import com.travelplanner.trip.domain.AgeGroup;
import com.travelplanner.trip.domain.Traveler;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Viajante visto pelo usuário: {@code me} se é o viajante vinculado à conta dele; {@code mine} se é
 * dele (só esses podem ser editados por ele).
 */
public record TravelerResponse(
    @NotNull Long id,
    @NotNull String name,
    @NotNull AgeGroup ageGroup,
    Integer birthYear,
    @NotNull List<String> nationalities,
    LocalDate passportExpiry,
    @NotNull Boolean me,
    @NotNull Boolean mine,
    @NotNull Long version) {

  static TravelerResponse from(Traveler traveler, long userId) {
    return new TravelerResponse(
        traveler.getId(),
        traveler.getName(),
        traveler.getAgeGroup(),
        traveler.getBirthYear(),
        traveler.getNationalities(),
        traveler.getPassportExpiry(),
        Objects.equals(traveler.getLinkedUserId(), userId),
        traveler.isOwnedBy(userId),
        traveler.getVersion());
  }
}
