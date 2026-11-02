-- Trechos de transporte da viagem. Um trecho é de deslocamento (origem → destino, onde NULL é a
-- cidade de origem do usuário, "casa") ou local, dentro de uma parada (local_stop_id; transfer
-- aeroporto → hotel, aluguel de carro), nunca os dois. Horários no relógio local de cada ponta,
-- com o fuso IANA ao lado (voos cruzam fusos). Excluir uma parada exclui os trechos ligados a ela.
CREATE TABLE transport_segment (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  trip_id             BIGINT        NOT NULL,
  type                ENUM ('FLIGHT', 'BUS', 'TRAIN', 'CAR', 'CAR_RENTAL', 'FERRY', 'TRANSFER',
                            'TAXI_RIDE', 'OTHER') NOT NULL,
  origin_stop_id      BIGINT,
  destination_stop_id BIGINT,
  local_stop_id       BIGINT,
  origin_label        VARCHAR(160),
  destination_label   VARCHAR(160),
  depart_at           DATETIME      NOT NULL,
  depart_tz           VARCHAR(64)   NOT NULL,
  arrive_at           DATETIME,
  arrive_tz           VARCHAR(64),
  carrier             VARCHAR(120),
  code                VARCHAR(40),
  booking_ref         VARCHAR(80),
  notes               TEXT,
  created_at          DATETIME(6)   NOT NULL,
  updated_at          DATETIME(6)   NOT NULL,
  version             BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_transport_segment_trip_depart (trip_id, depart_at),
  CONSTRAINT fk_segment_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_segment_origin FOREIGN KEY (origin_stop_id) REFERENCES stop (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_segment_destination FOREIGN KEY (destination_stop_id) REFERENCES stop (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_segment_local FOREIGN KEY (local_stop_id) REFERENCES stop (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
