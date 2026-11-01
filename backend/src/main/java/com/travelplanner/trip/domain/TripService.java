package com.travelplanner.trip.domain;

import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.auth.domain.HomeCity;
import com.travelplanner.shared.time.TimeProperties;
import com.travelplanner.trip.infra.TripMemberRepository;
import com.travelplanner.trip.infra.TripRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripService {

  /** Cores de viagem (ESP §13.7), usadas em rodízio quando a viagem é criada sem cor. */
  public static final List<String> COVER_COLORS =
      List.of("#0E7C86", "#E8613F", "#1D6FB8", "#7C5CC4", "#3F8F5A", "#C2477A");

  private static final Comparator<LocalDate> NULLS_LAST =
      Comparator.nullsLast(Comparator.naturalOrder());

  private static final Comparator<TripView> BY_START =
      Comparator.comparing((TripView v) -> v.trip().getStartDate(), NULLS_LAST)
          .thenComparing(v -> v.trip().getName(), String.CASE_INSENSITIVE_ORDER);

  private static final Comparator<TripView> BY_END_DESC =
      Comparator.comparing((TripView v) -> v.trip().getEndDate(), NULLS_LAST.reversed());

  private final TripRepository trips;
  private final TripMemberRepository members;
  private final TripAccessService access;
  private final AppUserService users;
  private final ApplicationEventPublisher events;
  private final TimeProperties timeProperties;
  private final Clock clock;

  /**
   * Viagens de que o usuário é membro. Próximas em ordem de início (sem data por último); passadas
   * da mais recente para a mais antiga.
   */
  @Transactional(readOnly = true)
  public List<TripView> list(long userId, TripScope scope) {
    LocalDate today = today(userId);
    return members.findAllWithTripByUserId(userId).stream()
        .map(m -> view(m.getTrip(), m.getRole(), today))
        .filter(
            v ->
                switch (scope) {
                  case UPCOMING -> v.status() != TripStatus.DONE;
                  case PAST -> v.status() == TripStatus.DONE;
                  case ALL -> true;
                })
        .sorted(scope == TripScope.PAST ? BY_END_DESC : BY_START)
        .toList();
  }

  @Transactional(readOnly = true)
  public TripView get(long tripId, long userId) {
    TripMember member = access.requireRole(tripId, userId, TripRole.VIEWER);
    return view(member.getTrip(), member.getRole(), today(userId));
  }

  /** Cria a viagem com o usuário como {@code OWNER}. */
  @Transactional
  public TripView create(long userId, TripDetails details) {
    if (details.coverColor() == null) {
      long owned = trips.countByOwnerId(userId);
      details = details.withCoverColor(COVER_COLORS.get((int) (owned % COVER_COLORS.size())));
    }
    Trip trip = trips.save(new Trip(userId, details));
    members.save(TripMember.of(trip, userId, TripRole.OWNER, clock.instant()));
    events.publishEvent(new TripChangedEvent(trip.getId()));
    return view(trip, TripRole.OWNER, today(userId));
  }

  /** Atualiza a viagem; {@code version} é a lida pelo cliente (divergente → 409). */
  @Transactional
  public TripView update(long tripId, long userId, long version, TripDetails details) {
    TripMember member = access.requireRole(tripId, userId, TripRole.EDITOR);
    Trip trip = member.getTrip();
    if (trip.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão da viagem divergente.");
    }
    // Sem cor no pedido, mantém a atual.
    trip.update(
        details.coverColor() == null ? details.withCoverColor(trip.getCoverColor()) : details);
    trips.saveAndFlush(trip);
    events.publishEvent(new TripChangedEvent(tripId));
    return view(trip, member.getRole(), today(userId));
  }

  /** Exclui a viagem; membros e demais filhos saem em cascata pelo banco. */
  @Transactional
  public void delete(long tripId, long userId) {
    TripMember member = access.requireRole(tripId, userId, TripRole.OWNER);
    trips.delete(member.getTrip());
    events.publishEvent(new TripChangedEvent(tripId));
  }

  private static TripView view(Trip trip, TripRole role, LocalDate today) {
    return new TripView(trip, role, trip.status(today));
  }

  /** "Hoje" no fuso da cidade de origem do usuário (ou no fuso padrão, se não houver). */
  private LocalDate today(long userId) {
    HomeCity home = users.get(userId).getHomeCity();
    ZoneId zone =
        home != null && home.timezone() != null
            ? ZoneId.of(home.timezone())
            : timeProperties.defaultZone();
    return LocalDate.now(clock.withZone(zone));
  }
}
