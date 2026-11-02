package com.travelplanner.trip.api;

import com.travelplanner.auth.domain.AppUserPrincipal;
import com.travelplanner.trip.domain.ShareLinkService;
import com.travelplanner.trip.domain.TripMemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partilha da viagem: membros (convite por e-mail, papel, remoção/saída) e links. A autorização
 * fica nos serviços.
 */
@RestController
@RequiredArgsConstructor
class SharingController {

  private final TripMemberService members;
  private final ShareLinkService links;

  @GetMapping("/api/v1/trips/{tripId}/members")
  List<MemberResponse> listMembers(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    long userId = principal.getUserId();
    return members.list(tripId, userId).stream().map(m -> MemberResponse.from(m, userId)).toList();
  }

  @PostMapping("/api/v1/trips/{tripId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  MemberResponse inviteMember(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody InviteMemberRequest request) {
    long userId = principal.getUserId();
    return MemberResponse.from(
        members.invite(tripId, userId, request.email(), request.role()), userId);
  }

  @PutMapping("/api/v1/trips/{tripId}/members/{memberId}")
  MemberResponse updateMember(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long memberId,
      @Valid @RequestBody UpdateMemberRequest request) {
    long userId = principal.getUserId();
    return MemberResponse.from(
        members.changeRole(tripId, memberId, userId, request.version(), request.role()), userId);
  }

  /** Remove o membro (dono) ou sai da viagem (o próprio membro). */
  @DeleteMapping("/api/v1/trips/{tripId}/members/{memberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteMember(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long memberId) {
    members.remove(tripId, memberId, principal.getUserId());
  }

  @GetMapping("/api/v1/trips/{tripId}/share-links")
  List<ShareLinkResponse> listShareLinks(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable long tripId) {
    return links.list(tripId, principal.getUserId()).stream().map(ShareLinkResponse::from).toList();
  }

  @PostMapping("/api/v1/trips/{tripId}/share-links")
  @ResponseStatus(HttpStatus.CREATED)
  ShareLinkResponse createShareLink(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @Valid @RequestBody CreateShareLinkRequest request) {
    return ShareLinkResponse.from(links.create(tripId, principal.getUserId(), request.role()));
  }

  @DeleteMapping("/api/v1/trips/{tripId}/share-links/{linkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void revokeShareLink(
      @AuthenticationPrincipal AppUserPrincipal principal,
      @PathVariable long tripId,
      @PathVariable long linkId) {
    links.revoke(tripId, linkId, principal.getUserId());
  }

  /** Entra na viagem pelo link (exige login). Link inexistente ou revogado → 404. */
  @PostMapping("/api/v1/share-links/{token}/join")
  JoinResponse joinTrip(
      @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable String token) {
    return JoinResponse.from(links.join(token, principal.getUserId()));
  }
}
