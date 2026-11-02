package com.travelplanner.auth.domain;

import com.travelplanner.auth.infra.AppUserRepository;
import com.travelplanner.shared.web.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserService {

  private final AppUserRepository repository;
  private final ApplicationEventPublisher events;

  /**
   * Cria o usuário no primeiro login ou atualiza e-mail, nome e foto nos seguintes. A chave é o
   * {@code sub} do Google, que não muda mesmo se o e-mail mudar. A criação publica {@link
   * UserCreatedEvent}; todo login publica {@link UserLoggedInEvent}.
   */
  @Transactional
  public AppUser upsertFromOidc(OidcUser oidcUser) {
    String email = oidcUser.getEmail();
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("O provedor não informou o e-mail do usuário.");
    }
    AppUser appUser =
        repository
            .findByGoogleSub(oidcUser.getSubject())
            .map(
                user -> {
                  user.updateIdentity(email, oidcUser.getFullName(), oidcUser.getPicture());
                  return user;
                })
            .orElseGet(
                () -> create(oidcUser.getSubject(), email, oidcUser.getFullName(), oidcUser));
    events.publishEvent(new UserLoggedInEvent(appUser.getId(), email));
    return appUser;
  }

  private AppUser create(String sub, String email, String name, OidcUser oidcUser) {
    AppUser user = repository.save(new AppUser(sub, email, name, oidcUser.getPicture()));
    String displayName = name == null || name.isBlank() ? email.split("@", 2)[0] : name.strip();
    events.publishEvent(new UserCreatedEvent(user.getId(), displayName, user.getNationality()));
    return user;
  }

  @Transactional(readOnly = true)
  public AppUser get(long userId) {
    return repository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
  }

  /** Usuário com o e-mail (sem diferenciar maiúsculas), o mais antigo se houver mais de um. */
  @Transactional(readOnly = true)
  public Optional<AppUser> findByEmail(String email) {
    return repository.findFirstByEmailOrderByIdAsc(email.strip());
  }

  /** Usuários pelos ids (os inexistentes são ignorados). */
  @Transactional(readOnly = true)
  public List<AppUser> getAll(Collection<Long> userIds) {
    return userIds.isEmpty() ? List.of() : repository.findAllById(userIds);
  }

  @Transactional
  public AppUser updateProfile(long userId, long version, String nationality, HomeCity homeCity) {
    AppUser user = get(userId);
    if (user.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do perfil divergente.");
    }
    user.updateProfile(nationality, homeCity);
    return repository.saveAndFlush(user);
  }
}
