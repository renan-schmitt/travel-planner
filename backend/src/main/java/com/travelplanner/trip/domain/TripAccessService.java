package com.travelplanner.trip.domain;

import com.travelplanner.shared.web.ForbiddenException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.infra.TripMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorização por viagem. Todo serviço que lê ou altera dados de uma viagem (a própria viagem,
 * paradas, transporte etc.) chama {@link #requireRole} <b>no serviço</b>, não só no controller.
 */
@Service("tripAccess")
@RequiredArgsConstructor
public class TripAccessService {

  private final TripMemberRepository members;

  /**
   * Garante que o usuário é membro da viagem com pelo menos {@code minRole} e devolve o vínculo.
   *
   * @throws NotFoundException se a viagem não existe ou o usuário não é membro (não revela que ela
   *     existe)
   * @throws ForbiddenException se é membro, mas com papel insuficiente
   */
  @Transactional(readOnly = true)
  public TripMember requireRole(long tripId, long userId, TripRole minRole) {
    TripMember member =
        members
            .findByTripIdAndUserId(tripId, userId)
            .orElseThrow(() -> new NotFoundException("Viagem não encontrada."));
    if (!member.getRole().atLeast(minRole)) {
      throw new ForbiddenException("Você não tem permissão para esta operação na viagem.");
    }
    return member;
  }
}
