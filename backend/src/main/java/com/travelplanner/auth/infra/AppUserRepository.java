package com.travelplanner.auth.infra;

import com.travelplanner.auth.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByGoogleSub(String googleSub);
}
