package com.travelplanner.trip.domain;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.auth.domain.AppUserService;
import com.travelplanner.auth.domain.UserLoggedInEvent;
import com.travelplanner.shared.web.ConflictException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.infra.TripMemberRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Membros da viagem: convite por e-mail, mudança de papel, remoção (ou saída) e o vínculo dos
 * convites pendentes quando a pessoa entra no sistema com o e-mail convidado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripMemberService {

  /**
   * Donos primeiro, depois editores e leitores; pendentes por último; depois por ordem de entrada.
   */
  private static final Comparator<MemberView> ORDER =
      Comparator.comparing((MemberView v) -> v.member().getRole(), Comparator.reverseOrder())
          .thenComparing(v -> v.member().isPending())
          .thenComparing(v -> v.member().getId());

  private final TripMemberRepository members;
  private final TripAccessService access;
  private final AppUserService users;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  @Transactional(readOnly = true)
  public List<MemberView> list(long tripId, long userId) {
    access.requireRole(tripId, userId, TripRole.VIEWER);
    List<TripMember> list = members.findByTripIdOrderByIdAsc(tripId);
    Map<Long, AppUser> byId =
        users
            .getAll(list.stream().map(TripMember::getUserId).filter(Objects::nonNull).toList())
            .stream()
            .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    return list.stream()
        .map(m -> new MemberView(m, m.getUserId() == null ? null : byId.get(m.getUserId())))
        .sorted(ORDER)
        .toList();
  }

  /**
   * Convida pelo e-mail. Se o e-mail já for de um usuário, ele vira membro na hora; senão fica um
   * convite pendente, vinculado quando a pessoa entrar com esse e-mail.
   *
   * @throws ConflictException se a pessoa já é membro ou já há convite para o e-mail
   */
  @Transactional
  public MemberView invite(long tripId, long userId, String email, TripRole role) {
    Trip trip = access.requireRole(tripId, userId, TripRole.OWNER).getTrip();
    String normalized = email.strip().toLowerCase(Locale.ROOT);
    members
        .findByTripIdAndInviteEmail(tripId, normalized)
        .ifPresent(
            m -> {
              throw new ConflictException(
                  m.isPending()
                      ? "Já existe um convite para este e-mail."
                      : "Esta pessoa já participa da viagem.");
            });
    AppUser existing = users.findByEmail(normalized).orElse(null);
    if (existing == null) {
      return new MemberView(members.saveAndFlush(TripMember.invite(trip, normalized, role)), null);
    }
    if (members.findByTripIdAndUserId(tripId, existing.getId()).isPresent()) {
      throw new ConflictException("Esta pessoa já participa da viagem.");
    }
    TripMember member =
        members.saveAndFlush(TripMember.of(trip, existing.getId(), role, clock.instant()));
    return new MemberView(member, existing);
  }

  /**
   * Muda o papel de um membro (ou de um convite pendente). Não deixa a viagem sem dono.
   *
   * @throws ConflictException ao rebaixar o último dono
   */
  @Transactional
  public MemberView changeRole(
      long tripId, long memberId, long userId, long version, TripRole role) {
    access.requireRole(tripId, userId, TripRole.OWNER);
    TripMember member = find(tripId, memberId);
    if (member.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do membro divergente.");
    }
    if (role != TripRole.OWNER && isLastOwner(member)) {
      throw new ConflictException("A viagem precisa de pelo menos um dono.");
    }
    member.changeRole(role);
    members.saveAndFlush(member);
    return new MemberView(
        member, member.getUserId() == null ? null : users.get(member.getUserId()));
  }

  /**
   * Remove um membro ou cancela um convite (dono), ou sai da viagem (o próprio membro).
   *
   * @throws ConflictException ao remover o último dono
   */
  @Transactional
  public void remove(long tripId, long memberId, long userId) {
    TripMember self = access.requireRole(tripId, userId, TripRole.VIEWER);
    TripMember member = find(tripId, memberId);
    boolean leaving = member.getId().equals(self.getId());
    if (!leaving) {
      access.requireRole(tripId, userId, TripRole.OWNER);
    }
    if (isLastOwner(member)) {
      throw new ConflictException("A viagem precisa de pelo menos um dono.");
    }
    members.delete(member);
    if (member.getUserId() != null) {
      events.publishEvent(new TripMemberRemovedEvent(tripId, member.getUserId()));
    }
  }

  /**
   * Junta o usuário à viagem com {@code role}; se já for membro, fica com o maior dos dois papéis.
   * Sem verificação de acesso (quem chama já validou o link ou o convite).
   */
  @Transactional
  public TripRole join(Trip trip, long userId, TripRole role) {
    return members
        .findByTripIdAndUserId(trip.getId(), userId)
        .map(
            existing -> {
              existing.changeRole(existing.getRole().max(role));
              return existing.getRole();
            })
        .orElseGet(
            () -> members.save(TripMember.of(trip, userId, role, clock.instant())).getRole());
  }

  /** No login, vincula os convites pendentes para o e-mail da pessoa. */
  @EventListener
  @Transactional
  public void onUserLoggedIn(UserLoggedInEvent event) {
    for (TripMember invite : members.findPendingByInviteEmail(event.email())) {
      var existing = members.findByTripIdAndUserId(invite.getTrip().getId(), event.userId());
      if (existing.isPresent()) {
        // Já entrou por link: fica com o maior papel e o convite deixa de existir.
        existing.get().changeRole(existing.get().getRole().max(invite.getRole()));
        members.delete(invite);
      } else {
        invite.accept(event.userId(), clock.instant());
      }
      log.info("Convite {} vinculado ao usuário {}", invite.getId(), event.userId());
    }
  }

  private TripMember find(long tripId, long memberId) {
    return members
        .findByIdAndTripId(memberId, tripId)
        .orElseThrow(() -> new NotFoundException("Membro não encontrado."));
  }

  private boolean isLastOwner(TripMember member) {
    return member.getRole() == TripRole.OWNER
        && !member.isPending()
        && members.countByTripIdAndRoleAndUserIdIsNotNull(member.getTrip().getId(), TripRole.OWNER)
            <= 1;
  }
}
