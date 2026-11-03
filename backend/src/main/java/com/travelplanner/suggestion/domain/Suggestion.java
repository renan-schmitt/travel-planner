package com.travelplanner.suggestion.domain;

import com.travelplanner.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Sugestão do motor de lacunas, única por (viagem, regra, chave lógica). */
@Getter
@Entity
@Table(name = "suggestion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Suggestion extends BaseEntity {

  @Column(name = "trip_id", nullable = false, updatable = false)
  private Long tripId;

  @Column(name = "rule_code", nullable = false, length = 64, updatable = false)
  private String ruleCode;

  @Column(name = "logical_key", nullable = false, length = 160, updatable = false)
  private String logicalKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Severity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SuggestionStatus status;

  @Column(name = "snoozed_until")
  private LocalDate snoozedUntil;

  @Column(name = "payload_json", nullable = false, columnDefinition = "json")
  private String payloadJson;

  public Suggestion(
      Long tripId, String ruleCode, String logicalKey, Severity severity, String payloadJson) {
    this.tripId = tripId;
    this.ruleCode = ruleCode;
    this.logicalKey = logicalKey;
    this.severity = severity;
    this.payloadJson = payloadJson;
    this.status = SuggestionStatus.PENDING;
  }

  /**
   * A regra produziu a sugestão de novo: atualiza severidade e parâmetros, devolve a adiada vencida
   * a {@code PENDING} e reabre uma aceita/dispensada se a severidade <b>subiu</b> (ex.: passaporte
   * "informe a validade" que virou "vence antes da volta").
   *
   * @param payloadJson parâmetros novos, ou {@code null} se não mudaram
   * @return se algo mudou
   */
  public boolean refresh(Severity severity, String payloadJson, LocalDate today) {
    boolean changed = false;
    if (severity.compareTo(this.severity) > 0
        && (status == SuggestionStatus.ACCEPTED || status == SuggestionStatus.DISMISSED)) {
      reopen();
      changed = true;
    }
    if (snoozeExpired(today)) {
      reopen();
      changed = true;
    }
    if (this.severity != severity) {
      this.severity = severity;
      changed = true;
    }
    if (payloadJson != null) {
      this.payloadJson = payloadJson;
      changed = true;
    }
    return changed;
  }

  /** Estado visto pelo usuário hoje: a adiada cujo prazo chegou já conta como pendente. */
  public SuggestionStatus effectiveStatus(LocalDate today) {
    return snoozeExpired(today) ? SuggestionStatus.PENDING : status;
  }

  /** Se pode ser apagada quando a regra deixa de produzi-la (aceitas e dispensadas ficam). */
  public boolean open() {
    return status == SuggestionStatus.PENDING || status == SuggestionStatus.SNOOZED;
  }

  public void accept() {
    this.status = SuggestionStatus.ACCEPTED;
    this.snoozedUntil = null;
  }

  public void dismiss() {
    this.status = SuggestionStatus.DISMISSED;
    this.snoozedUntil = null;
  }

  public void snooze(LocalDate until) {
    this.status = SuggestionStatus.SNOOZED;
    this.snoozedUntil = until;
  }

  public void reopen() {
    this.status = SuggestionStatus.PENDING;
    this.snoozedUntil = null;
  }

  private boolean snoozeExpired(LocalDate today) {
    return status == SuggestionStatus.SNOOZED
        && snoozedUntil != null
        && !today.isBefore(snoozedUntil);
  }
}
