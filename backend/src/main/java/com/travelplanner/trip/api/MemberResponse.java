package com.travelplanner.trip.api;

import com.travelplanner.auth.domain.AppUser;
import com.travelplanner.trip.domain.MemberView;
import com.travelplanner.trip.domain.TripMember;
import com.travelplanner.trip.domain.TripRole;
import jakarta.validation.constraints.NotNull;

/**
 * Membro da viagem. {@code pending}: convite ainda não aceito (sem conta vinculada; só {@code
 * email}). {@code me}: é quem pede.
 */
public record MemberResponse(
    @NotNull Long id,
    @NotNull String email,
    String name,
    String pictureUrl,
    @NotNull TripRole role,
    @NotNull Boolean pending,
    @NotNull Boolean me,
    @NotNull Long version) {

  static MemberResponse from(MemberView view, long currentUserId) {
    TripMember member = view.member();
    AppUser user = view.user();
    return new MemberResponse(
        member.getId(),
        user != null ? user.getEmail() : member.getInviteEmail(),
        user != null ? user.getName() : null,
        user != null ? user.getPictureUrl() : null,
        member.getRole(),
        member.isPending(),
        member.getUserId() != null && member.getUserId() == currentUserId,
        member.getVersion());
  }
}
