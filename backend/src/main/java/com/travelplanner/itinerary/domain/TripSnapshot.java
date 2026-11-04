package com.travelplanner.itinerary.domain;

import com.travelplanner.auth.domain.HomeCity;
import com.travelplanner.trip.domain.TravelerDetails;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Foto imutável de uma viagem (viagem, dono, paradas, trechos, hospedagens, viajantes e lugares
 * favoritos), montada pelo {@link TripSnapshotLoader}. É a entrada da lógica pura da timeline
 * ({@link ItineraryBuilder}) e do motor de lacunas (Fase 14).
 */
public record TripSnapshot(
    TripInfo trip,
    Owner owner,
    List<StopEntry> stops,
    List<SegmentEntry> segments,
    List<LodgingEntry> lodgings,
    List<TravelerEntry> travelers,
    List<SavedPlaceEntry> savedPlaces) {

  public TripSnapshot {
    stops = List.copyOf(stops);
    segments = List.copyOf(segments);
    lodgings = List.copyOf(lodgings);
    travelers = List.copyOf(travelers);
    savedPlaces = List.copyOf(savedPlaces);
  }

  /** Viagem; datas nulas enquanto não houver datas manuais nem roteiro. */
  public record TripInfo(long id, String name, LocalDate startDate, LocalDate endDate) {}

  /** Dono da viagem; {@code homeCity} é a "casa" dos trechos sem parada (pode ser nula). */
  public record Owner(long userId, String nationality, HomeCity homeCity) {}

  /** Parada na ordem do roteiro ({@code seq}). */
  public record StopEntry(long id, int seq, StopDetails details) {}

  public record SegmentEntry(long id, SegmentDetails details) {}

  /** Hospedagem; {@code placeId} nulo quando cadastrada à mão (sem a busca de hotéis). */
  public record LodgingEntry(long id, long stopId, String placeId, LodgingDetails details) {}

  public record TravelerEntry(long id, TravelerDetails details) {}

  /** Lugar favorito de uma parada. */
  public record SavedPlaceEntry(long id, long stopId, String placeId, SavedPlaceKind kind) {}

  public Optional<StopEntry> stop(Long stopId) {
    return stopId == null
        ? Optional.empty()
        : stops.stream().filter(s -> s.id() == stopId).findFirst();
  }
}
