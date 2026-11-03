package com.travelplanner.suggestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelplanner.IntegrationTest;
import com.travelplanner.auth.TestUsers;
import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.itinerary.domain.StopDetails;
import com.travelplanner.itinerary.domain.StopService;
import com.travelplanner.trip.domain.TripDetails;
import com.travelplanner.trip.domain.TripService;
import com.travelplanner.trip.domain.TripStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SuggestionStartupIT extends IntegrationTest {

  @Autowired AppUserService users;
  @Autowired TripService tripService;
  @Autowired StopService stops;
  @Autowired SuggestionStartupReconciler reconciler;
  @Autowired JdbcTemplate jdbc;

  @Test
  void tripsNeverEvaluatedGetTheirSuggestionsOnStartup() {
    AppUser ana =
        users.upsertFromOidc(TestUsers.googleUser(UUID.randomUUID() + "@example.com", "Ana"));
    long tripId =
        tripService
            .create(ana.getId(), new TripDetails("Portugal", null, null, TripStatus.PLANNING, null))
            .trip()
            .getId();
    stops.create(
        tripId,
        ana.getId(),
        new StopDetails(
            null,
            "Lisboa",
            "PT",
            null,
            null,
            "Europe/Lisbon",
            LocalDate.parse("2099-03-11"),
            LocalDate.parse("2099-03-14"),
            null));
    // Simula uma viagem de antes do motor: nenhuma sugestão gravada.
    jdbc.update("delete from suggestion where trip_id = ?", tripId);

    assertThat(reconciler.reconcileAll()).isPositive();

    assertThat(
            jdbc.queryForList(
                "select rule_code from suggestion where trip_id = ?", String.class, tripId))
        .contains("SEGURO_VIAGEM", "NOITE_SEM_HOSPEDAGEM", "DESTINO_SEM_TRANSPORTE");
  }
}
