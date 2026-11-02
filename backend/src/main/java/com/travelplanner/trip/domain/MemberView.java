package com.travelplanner.trip.domain;

import com.travelplanner.auth.domain.AppUser;

/** Membro da viagem com o usuário vinculado ({@code null} enquanto o convite está pendente). */
public record MemberView(TripMember member, AppUser user) {}
