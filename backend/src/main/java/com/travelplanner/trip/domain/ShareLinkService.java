package com.travelplanner.trip.domain;

import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.infra.TripRepository;
import com.travelplanner.trip.infra.TripShareLinkRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links de partilha. O link exige login: quem o abre entra como membro com o papel do link, sem
 * expor dados da viagem publicamente.
 */
@Service
@RequiredArgsConstructor
public class ShareLinkService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final TripShareLinkRepository links;
  private final TripRepository trips;
  private final TripAccessService access;
  private final TripMemberService members;
  private final Clock clock;

  /** Links ativos (não revogados) da viagem. */
  @Transactional(readOnly = true)
  public List<TripShareLink> list(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.OWNER);
    return links.findByTripIdAndRevokedAtIsNullOrderByIdAsc(tripId);
  }

  @Transactional
  public TripShareLink create(long tripId, long userId, TripRole role) {
    access.requireRole(tripId, userId, TripRole.OWNER);
    if (role == TripRole.OWNER) {
      throw new InvalidRequestException("role", "O link só pode dar papel de editor ou leitor.");
    }
    return links.saveAndFlush(new TripShareLink(tripId, newToken(), role, userId));
  }

  @Transactional
  public void revoke(long tripId, long linkId, long userId) {
    access.requireRole(tripId, userId, TripRole.OWNER);
    TripShareLink link =
        links
            .findByIdAndTripId(linkId, tripId)
            .orElseThrow(() -> new NotFoundException("Link não encontrado."));
    link.revoke(clock.instant());
  }

  /**
   * Entra na viagem pelo link. Quem já é membro com papel maior mantém o papel.
   *
   * @throws NotFoundException se o link não existe ou foi revogado
   */
  @Transactional
  public JoinResult join(String token, long userId) {
    TripShareLink link =
        links
            .findByTokenAndRevokedAtIsNull(token)
            .orElseThrow(() -> new NotFoundException("Link inválido ou revogado."));
    Trip trip =
        trips
            .findById(link.getTripId())
            .orElseThrow(() -> new NotFoundException("Link inválido ou revogado."));
    return new JoinResult(trip.getId(), members.join(trip, userId, link.getRole()));
  }

  static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
