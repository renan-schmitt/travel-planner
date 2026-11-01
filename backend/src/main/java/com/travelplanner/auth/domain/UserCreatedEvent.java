package com.travelplanner.auth.domain;

/**
 * Publicado na mesma transação em que um usuário é criado (primeiro login). Ouvintes síncronos
 * (ex.: o viajante "eu", no módulo {@code trip}) rodam dentro dela.
 */
public record UserCreatedEvent(long userId, String displayName, String nationality) {}
