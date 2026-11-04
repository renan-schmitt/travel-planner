package com.travelplanner.requirements.domain;

import com.travelplanner.auth.domain.AdminAccess;
import com.travelplanner.requirements.infra.DestinationRequirementRepository;
import com.travelplanner.shared.time.TimeProperties;
import com.travelplanner.shared.web.InvalidRequestException;
import com.travelplanner.shared.web.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administração da base de requisitos (só administradores). Toda alteração publica {@link
 * RequirementsChangedEvent} do país (e do país antigo, se ele mudou).
 */
@Service
@RequiredArgsConstructor
public class RequirementAdminService {

  private final DestinationRequirementRepository requirements;
  private final RequirementCatalog catalog;
  private final AdminAccess admin;
  private final ApplicationEventPublisher events;
  private final TimeProperties time;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<DestinationRequirement> list(
      long userId, String countryCode, RequirementStatus status) {
    admin.requireAdmin(userId);
    return requirements.search(
        countryCode == null ? null : countryCode.toUpperCase(Locale.ROOT), status);
  }

  @Transactional(readOnly = true)
  public DestinationRequirement get(long userId, long id) {
    admin.requireAdmin(userId);
    return find(id);
  }

  @Transactional
  public DestinationRequirement create(long userId, RequirementDetails details) {
    admin.requireAdmin(userId);
    RequirementDetails normalized = validate(details);
    DestinationRequirement requirement =
        requirements.saveAndFlush(new DestinationRequirement(normalized, RequirementSource.MANUAL));
    events.publishEvent(new RequirementsChangedEvent(requirement.getCountryCode()));
    return requirement;
  }

  /** Editar um publicado mantém o status e a data da verificação (publicar de novo confirma). */
  @Transactional
  public DestinationRequirement update(
      long userId, long id, long version, RequirementDetails details) {
    admin.requireAdmin(userId);
    DestinationRequirement requirement = find(id);
    if (requirement.getVersion() != version) {
      throw new OptimisticLockingFailureException("Versão do requisito divergente.");
    }
    RequirementDetails normalized = validate(details);
    String previousCountry = requirement.getCountryCode();
    requirement.apply(normalized);
    requirements.saveAndFlush(requirement);
    events.publishEvent(new RequirementsChangedEvent(requirement.getCountryCode()));
    if (!previousCountry.equals(requirement.getCountryCode())) {
      events.publishEvent(new RequirementsChangedEvent(previousCountry));
    }
    return requirement;
  }

  @Transactional
  public void delete(long userId, long id) {
    admin.requireAdmin(userId);
    DestinationRequirement requirement = find(id);
    requirements.delete(requirement);
    requirements.flush();
    events.publishEvent(new RequirementsChangedEvent(requirement.getCountryCode()));
  }

  /** Publica ou confirma de novo ({@code verifiedAt} = hoje, {@code verifiedBy} = o admin). */
  @Transactional
  public DestinationRequirement publish(long userId, long id) {
    admin.requireAdmin(userId);
    DestinationRequirement requirement = find(id);
    requirement.publish(userId, LocalDate.now(clock.withZone(time.defaultZone())));
    requirements.saveAndFlush(requirement);
    events.publishEvent(new RequirementsChangedEvent(requirement.getCountryCode()));
    return requirement;
  }

  @Transactional
  public DestinationRequirement unpublish(long userId, long id) {
    admin.requireAdmin(userId);
    DestinationRequirement requirement = find(id);
    requirement.unpublish();
    requirements.saveAndFlush(requirement);
    events.publishEvent(new RequirementsChangedEvent(requirement.getCountryCode()));
    return requirement;
  }

  private DestinationRequirement find(long id) {
    return requirements
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Requisito não encontrado."));
  }

  private RequirementDetails validate(RequirementDetails d) {
    String country = d.countryCode().toUpperCase(Locale.ROOT);
    String nationality = d.nationality().toUpperCase(Locale.ROOT);
    if (!catalog.countryExists(country)) {
      throw new InvalidRequestException("countryCode", "País desconhecido.");
    }
    if (!catalog.countryExists(nationality)) {
      throw new InvalidRequestException("nationality", "Nacionalidade desconhecida.");
    }
    return new RequirementDetails(
        country,
        nationality,
        d.type(),
        d.title().trim(),
        d.description() == null || d.description().isBlank() ? null : d.description().trim(),
        d.leadDays(),
        d.officialUrl().trim());
  }
}
