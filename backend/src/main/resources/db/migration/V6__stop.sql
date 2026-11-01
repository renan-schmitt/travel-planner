-- Paradas (destinos) da viagem, na ordem do roteiro (seq). Cidade vem do autocomplete do Places
-- (city_place_id) ou é digitada à mão (sem place_id nem coordenadas). Guardar nome, país,
-- coordenadas e fuso é aceitável: são dados do roteiro do usuário, não catálogo do Places.
CREATE TABLE stop (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  trip_id        BIGINT        NOT NULL,
  seq            INT           NOT NULL,
  city_place_id  VARCHAR(255),
  city_name      VARCHAR(160)  NOT NULL,
  country_code   CHAR(2)       NOT NULL,
  lat            DECIMAL(9, 6),
  lng            DECIMAL(9, 6),
  timezone       VARCHAR(64)   NOT NULL,
  arrival_date   DATE          NOT NULL,
  departure_date DATE          NOT NULL,
  notes          TEXT,
  created_at     DATETIME(6)   NOT NULL,
  updated_at     DATETIME(6)   NOT NULL,
  version        BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_stop_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT uk_stop_trip_seq UNIQUE (trip_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Datas da viagem: manuais (digitadas no formulário) ou derivadas das paradas (mínimo da chegada,
-- máximo da saída). Viagens que já têm alguma data ficam como manuais.
ALTER TABLE trip ADD COLUMN dates_manual BOOLEAN NOT NULL DEFAULT FALSE AFTER end_date;
UPDATE trip SET dates_manual = (start_date IS NOT NULL OR end_date IS NOT NULL);
