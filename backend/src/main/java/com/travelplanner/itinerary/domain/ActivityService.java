package com.travelplanner.itinerary.domain;

import com.travelplanner.itinerary.infra.ActivityRepository;
import com.travelplanner.itinerary.infra.SavedPlaceRepository;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.domain.Trip;
import com.travelplanner.trip.domain.TripAccessService;
import com.travelplanner.trip.domain.TripRole;
import com.travelplanner.trip.infra.TripRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atividades da timeline. Leitura exige {@code VIEWER}; alterações, {@code EDITOR}. O dia precisa
 * estar dentro das datas da viagem; o favorito ligado, numa parada da viagem. Atividades não
 * alimentam as regras de lacunas, então não publicam {@code TripChangedEvent}.
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

  private final ActivityRepository activities;
  private final SavedPlaceRepository savedPlaces;
  private final TripRepository trips;
  private final TripAccessService access;

  @Transactional(readOnly = true)
  public List<Activity> list(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return activities.findByTripIdOrderByDayAscSortOrderAscIdAsc(tripId);
  }

  /** Nova atividade no fim do dia. Sem título, usa o nome do favorito ligado. */
  @Transactional
  public Activity create(long tripId, long userId, ActivityDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    ActivityDetails checked = check(tripId, details);
    return activities.saveAndFlush(
        new Activity(tripId, checked, activities.nextSortOrder(tripId, checked.day())));
  }

  /** Mudar o dia leva a atividade para o fim do novo dia. */
  @Transactional
  public Activity update(
      long tripId, long activityId, long userId, long version, ActivityDetails details) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Activity activity = find(tripId, activityId);
    if (activity.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão da atividade divergente.");
    }
    ActivityDetails checked = check(tripId, details);
    if (!checked.day().equals(activity.getDay())) {
      activity.moveTo(checked.day(), activities.nextSortOrder(tripId, checked.day()));
    }
    activity.update(checked);
    return activities.saveAndFlush(activity);
  }

  @Transactional
  public void delete(long tripId, long activityId, long userId) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    activities.delete(find(tripId, activityId));
    activities.flush();
  }

  /**
   * Nova ordem das atividades de {@code day}. {@code activityIds} precisa ter todas as atividades
   * que já estão no dia; pode trazer atividades de outros dias, que passam para ele (arrastar entre
   * dias). Devolve as atividades do dia na nova ordem.
   */
  @Transactional
  public List<Activity> reorder(long tripId, long userId, LocalDate day, List<Long> activityIds) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Trip trip = trip(tripId);
    ActivityDetails.checkDay(day, trip.getStartDate(), trip.getEndDate());
    Map<Long, Activity> byId =
        activities.findByTripIdOrderByDayAscSortOrderAscIdAsc(tripId).stream()
            .collect(Collectors.toMap(Activity::getId, Function.identity()));
    boolean valid =
        new HashSet<>(activityIds).size() == activityIds.size()
            && byId.keySet().containsAll(activityIds)
            && byId.values().stream()
                .filter(a -> a.getDay().equals(day))
                .allMatch(a -> activityIds.contains(a.getId()));
    if (!valid) {
      throw new InvalidRequestException(
          "activityIds",
          "A lista precisa ter todas as atividades do dia, sem repetição, e só atividades da"
              + " viagem.");
    }
    List<Activity> ordered = activityIds.stream().map(byId::get).toList();
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).moveTo(day, i);
    }
    activities.saveAllAndFlush(ordered);
    return ordered;
  }

  private ActivityDetails check(long tripId, ActivityDetails details) {
    Trip trip = trip(tripId);
    details.validate(trip.getStartDate(), trip.getEndDate());
    String title = details.title();
    if (details.savedPlaceId() != null) {
      SavedPlace place =
          savedPlaces
              .findByIdAndTripId(details.savedPlaceId(), tripId)
              .orElseThrow(
                  () -> new InvalidRequestException("savedPlaceId", "Favorito não encontrado."));
      if (title == null) {
        title = place.getName();
      }
    }
    if (title == null) {
      throw new InvalidRequestException("title", "Informe o que vai fazer.");
    }
    return details.withTitle(title);
  }

  private Trip trip(long tripId) {
    return trips
        .findById(tripId)
        .orElseThrow(() -> new NotFoundException("Viagem não encontrada."));
  }

  private Activity find(long tripId, long activityId) {
    return activities
        .findByIdAndTripId(activityId, tripId)
        .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));
  }
}
