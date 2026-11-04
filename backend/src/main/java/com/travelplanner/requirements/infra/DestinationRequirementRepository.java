package com.travelplanner.requirements.infra;

import com.travelplanner.requirements.domain.DestinationRequirement;
import com.travelplanner.requirements.domain.RequirementStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DestinationRequirementRepository
    extends JpaRepository<DestinationRequirement, Long> {

  List<DestinationRequirement> findByCountryCodeInAndStatus(
      Collection<String> countryCodes, RequirementStatus status);

  /** Filtros opcionais (nulo = todos), por país, nacionalidade, tipo e título. */
  @Query(
      "select r from DestinationRequirement r where (:countryCode is null or r.countryCode ="
          + " :countryCode) and (:status is null or r.status = :status) order by r.countryCode,"
          + " r.nationality, r.type, r.title, r.id")
  List<DestinationRequirement> search(String countryCode, RequirementStatus status);
}
