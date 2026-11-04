package com.travelplanner.requirements.infra;

import com.travelplanner.requirements.domain.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {

  List<Country> findAllByOrderByNamePt();
}
