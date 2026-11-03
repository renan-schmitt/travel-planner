-- Sugestões do motor de lacunas (Fase 14). Uma linha por (viagem, regra, chave lógica): o motor
-- reconcilia a cada mudança da viagem, criando as novas como PENDING, mantendo ACCEPTED/DISMISSED e
-- apagando as PENDING/SNOOZED que a regra deixou de produzir (lacuna resolvida). payload_json guarda
-- os parâmetros da mensagem (montada no frontend via i18n) e o item de checklist sugerido.
CREATE TABLE suggestion (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  trip_id       BIGINT      NOT NULL,
  rule_code     VARCHAR(64)  NOT NULL,
  logical_key   VARCHAR(160) NOT NULL,
  severity      ENUM ('INFO', 'WARNING', 'CRITICAL') NOT NULL,
  status        ENUM ('PENDING', 'ACCEPTED', 'SNOOZED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
  snoozed_until DATE,
  payload_json  JSON        NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  version       BIGINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_suggestion_rule UNIQUE (trip_id, rule_code, logical_key),
  KEY ix_suggestion_trip_status (trip_id, status),
  CONSTRAINT fk_suggestion_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
