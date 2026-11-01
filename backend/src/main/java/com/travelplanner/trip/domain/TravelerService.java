package com.travelplanner.trip.domain;

import com.travelplanner.auth.domain.UserCreatedEvent;
import com.travelplanner.shared.web.ConflictException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.infra.TravelerRepository;
import com.travelplanner.trip.infra.TripTravelerRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Viajantes do usuário (reaproveitados entre viagens) e quem vai em cada viagem. Cada usuário só vê
 * e edita os próprios viajantes; numa viagem aparecem também os trazidos por outros membros.
 */
@Service
@RequiredArgsConstructor
public class TravelerService {

  private static final int NAME_MAX = 120;

  private final TravelerRepository travelers;
  private final TripTravelerRepository tripTravelers;
  private final TripAccessService access;
  private final ApplicationEventPublisher events;

  /** Cria o viajante "eu" do usuário recém-criado, na mesma transação. */
  @EventListener
  @Transactional
  public void onUserCreated(UserCreatedEvent event) {
    String name = event.displayName();
    travelers.save(
        Traveler.self(
            event.userId(),
            name.length() > NAME_MAX ? name.substring(0, NAME_MAX) : name,
            event.nationality()));
  }

  /** Viajantes do usuário: "eu" primeiro, depois por nome. */
  @Transactional(readOnly = true)
  public List<Traveler> list(long userId) {
    return sorted(travelers.findByOwnerUserId(userId), userId);
  }

  @Transactional
  public Traveler create(long userId, TravelerDetails details) {
    return travelers.save(new Traveler(userId, details));
  }

  /** Atualiza um viajante do usuário; avisa as viagens em que ele está (regras de documentos). */
  @Transactional
  public Traveler update(long travelerId, long userId, long version, TravelerDetails details) {
    Traveler traveler = own(travelerId, userId);
    if (traveler.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do viajante divergente.");
    }
    traveler.update(details);
    travelers.saveAndFlush(traveler);
    publishForTripsOf(travelerId);
    return traveler;
  }

  /** Exclui um viajante do usuário (sai também das viagens). O "eu" não pode ser excluído. */
  @Transactional
  public void delete(long travelerId, long userId) {
    Traveler traveler = own(travelerId, userId);
    if (traveler.isSelf()) {
      throw new ConflictException("Seu próprio viajante não pode ser excluído.");
    }
    publishForTripsOf(travelerId);
    travelers.delete(traveler);
  }

  @Transactional(readOnly = true)
  public List<Traveler> listForTrip(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    return sorted(travelers.findByTripId(tripId), userId);
  }

  /**
   * Substitui quem vai na viagem. Aceita viajantes do próprio usuário ou que já estão na viagem
   * (trazidos por outro membro); qualquer outro id dá 404, sem revelar que ele existe.
   */
  @Transactional
  public List<Traveler> setForTrip(long tripId, long userId, Collection<Long> travelerIds) {
    access.requireRole(tripId, userId, TripRole.EDITOR);
    Set<Long> requested = new LinkedHashSet<>(travelerIds);
    Map<Long, Traveler> current =
        travelers.findByTripId(tripId).stream()
            .collect(Collectors.toMap(Traveler::getId, Function.identity()));
    Map<Long, Traveler> candidates =
        travelers.findAllById(requested).stream()
            .filter(t -> current.containsKey(t.getId()) || t.isOwnedBy(userId))
            .collect(Collectors.toMap(Traveler::getId, Function.identity()));
    if (!candidates.keySet().containsAll(requested)) {
      throw new NotFoundException("Viajante não encontrado.");
    }
    tripTravelers.deleteByTripId(tripId);
    tripTravelers.saveAll(requested.stream().map(id -> new TripTraveler(tripId, id)).toList());
    events.publishEvent(new TripChangedEvent(tripId));
    return sorted(candidates.values(), userId);
  }

  /** Coloca o viajante "eu" do usuário na viagem recém-criada (se ele existir). */
  @Transactional
  public void addSelfToTrip(long tripId, long userId) {
    travelers
        .findSelf(userId)
        .ifPresent(self -> tripTravelers.save(new TripTraveler(tripId, self.getId())));
  }

  private Traveler own(long travelerId, long userId) {
    return travelers
        .findByIdAndOwnerUserId(travelerId, userId)
        .orElseThrow(() -> new NotFoundException("Viajante não encontrado."));
  }

  private void publishForTripsOf(long travelerId) {
    tripTravelers
        .findTripIdsByTravelerId(travelerId)
        .forEach(tripId -> events.publishEvent(new TripChangedEvent(tripId)));
  }

  /** O viajante vinculado ao usuário primeiro; depois por nome. */
  private static List<Traveler> sorted(Collection<Traveler> list, long userId) {
    return list.stream()
        .sorted(
            Comparator.comparing((Traveler t) -> !Long.valueOf(userId).equals(t.getLinkedUserId()))
                .thenComparing(Traveler::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }
}
