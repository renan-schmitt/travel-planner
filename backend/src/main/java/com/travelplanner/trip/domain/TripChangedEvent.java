package com.travelplanner.trip.domain;

/**
 * Publicado por qualquer serviço que altere dados da viagem (inclusive a exclusão). Ouvintes que
 * dependem do banco devem usar {@code @TransactionalEventListener(phase = AFTER_COMMIT)} (ex.: o
 * motor de lacunas, Fase 14).
 */
public record TripChangedEvent(long tripId) {}
