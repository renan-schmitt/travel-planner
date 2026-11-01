package com.travelplanner.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.travelplanner.shared.web.ForbiddenException;
import com.travelplanner.shared.web.NotFoundException;
import com.travelplanner.trip.infra.TripMemberRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Matriz papel do membro × papel exigido pela operação. */
class TripAccessServiceTest {

  private static final long TRIP_ID = 10;
  private static final long USER_ID = 20;

  private final TripMemberRepository members = mock(TripMemberRepository.class);
  private final TripAccessService access = new TripAccessService(members);

  @ParameterizedTest(name = "{0} exigindo {1} → permitido={2}")
  @CsvSource({
    "VIEWER, VIEWER, true",
    "VIEWER, EDITOR, false",
    "VIEWER, OWNER,  false",
    "EDITOR, VIEWER, true",
    "EDITOR, EDITOR, true",
    "EDITOR, OWNER,  false",
    "OWNER,  VIEWER, true",
    "OWNER,  EDITOR, true",
    "OWNER,  OWNER,  true",
  })
  void roleMatrix(TripRole role, TripRole required, boolean allowed) {
    TripMember member = TripMember.of(null, USER_ID, role, Instant.EPOCH);
    when(members.findByTripIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(member));

    if (allowed) {
      assertThat(access.requireRole(TRIP_ID, USER_ID, required)).isSameAs(member);
    } else {
      assertThatThrownBy(() -> access.requireRole(TRIP_ID, USER_ID, required))
          .isInstanceOf(ForbiddenException.class);
    }
  }

  @Test
  void nonMemberGetsNotFound() {
    when(members.findByTripIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.empty());

    for (TripRole required : TripRole.values()) {
      assertThatThrownBy(() -> access.requireRole(TRIP_ID, USER_ID, required))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
