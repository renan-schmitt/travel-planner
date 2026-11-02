package com.travelplanner.auth.domain;

/**
 * Publicado na mesma transação de todo login (Google ou dev), depois do {@link UserCreatedEvent}
 * quando o usuário é novo. Ouvintes síncronos (ex.: vincular convites pendentes, no módulo {@code
 * trip}) rodam dentro dela.
 */
public record UserLoggedInEvent(long userId, String email) {}
