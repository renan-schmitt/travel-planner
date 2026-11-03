package com.travelplanner.trip.domain;

/**
 * Publicado (na mesma transação) quando um membro com conta sai ou é removido da viagem; ex.: o
 * checklist tira essa pessoa dos itens de que ela era responsável.
 */
public record TripMemberRemovedEvent(long tripId, long userId) {}
