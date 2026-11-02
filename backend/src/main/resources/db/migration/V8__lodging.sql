-- Hospedagens de uma parada (pode haver mais de uma: dois hotéis na mesma cidade): hotel/pousada,
-- aluguel (apartamento, casa de temporada) ou casa de amigo/parente (sem regime, preço nem
-- reserva). Check-in e check-out dentro (ou no limite) das datas da parada. place_id fica nulo
-- até a busca no Places (Fase 16); o nome é texto livre. Faixa de preço 1–4 ($–$$$$) com a origem da informação.
-- Excluir a parada exclui as hospedagens dela.
CREATE TABLE lodging (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  stop_id           BIGINT       NOT NULL,
  kind              ENUM ('HOTEL', 'RENTAL', 'FRIENDS_HOME') NOT NULL DEFAULT 'HOTEL',
  place_id          VARCHAR(255),
  name              VARCHAR(160) NOT NULL,
  address           VARCHAR(300),
  check_in          DATE         NOT NULL,
  check_out         DATE         NOT NULL,
  board_type        ENUM ('ROOM_ONLY', 'BREAKFAST', 'HALF_BOARD', 'FULL_BOARD', 'ALL_INCLUSIVE'),
  price_tier        TINYINT,
  price_tier_source ENUM ('PLACES', 'HEURISTIC', 'AI', 'USER'),
  booking_ref       VARCHAR(80),
  notes             TEXT,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  version           BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_lodging_stop_check_in (stop_id, check_in),
  CONSTRAINT fk_lodging_stop FOREIGN KEY (stop_id) REFERENCES stop (id) ON DELETE CASCADE,
  CONSTRAINT ck_lodging_dates CHECK (check_out > check_in),
  CONSTRAINT ck_lodging_price_tier CHECK (price_tier BETWEEN 1 AND 4)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
