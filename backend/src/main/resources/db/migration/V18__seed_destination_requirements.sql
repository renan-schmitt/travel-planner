-- Seed inicial de requisitos para brasileiros (ESP §3.4), todos como DRAFT: o admin confirma cada um
-- na fonte oficial e publica pela tela /admin/requirements. Nada aqui vale para as viagens antes
-- disso. Datas de criação fixas (dados de referência).

INSERT INTO destination_requirement
  (country_code, nationality, type, title, description, lead_days, official_url, status, source,
   created_at, updated_at)
VALUES
  ('DO', 'BR', 'ENTRY_FORM', 'Preencher o e-Ticket (República Dominicana)',
   'Formulário eletrônico de entrada e saída, gratuito, obrigatório para cada viajante (inclusive crianças). Preencha até 72 horas antes do voo e leve o QR code.',
   3, 'https://eticket.migracion.gob.do/', 'DRAFT', 'MANUAL', NOW(6), NOW(6)),
  ('US', 'BR', 'VISA', 'Visto americano B1/B2',
   'Brasileiros precisam de visto de turismo (B1/B2) válido. O agendamento da entrevista pode levar meses: comece cedo.',
   NULL, 'https://travel.state.gov/content/travel/en/us-visas/tourism-visit/visitor.html', 'DRAFT', 'MANUAL', NOW(6), NOW(6)),
  ('GB', 'BR', 'ETA', 'Autorização eletrônica de viagem (ETA) do Reino Unido',
   'Pedido online pelo aplicativo ou site do governo britânico. A resposta costuma sair em até 3 dias úteis.',
   3, 'https://www.gov.uk/guidance/apply-for-an-electronic-travel-authorisation-eta', 'DRAFT', 'MANUAL', NOW(6), NOW(6)),
  ('CA', 'BR', 'ETA', 'eTA ou visto do Canadá',
   'Brasileiros que tiveram visto canadense nos últimos 10 anos ou têm visto americano válido podem pedir a eTA (só para chegada de avião); os demais precisam de visto.',
   7, 'https://www.canada.ca/en/immigration-refugees-citizenship/services/visit-canada/eta.html', 'DRAFT', 'MANUAL', NOW(6), NOW(6));

-- Espaço Schengen: isenção de visto para turismo e o ETIAS (publicar só quando estiver em vigor).
INSERT INTO destination_requirement
  (country_code, nationality, type, title, description, lead_days, official_url, status, source,
   created_at, updated_at)
SELECT code, 'BR', 'OTHER', 'Isenção de visto no espaço Schengen (até 90 dias)',
       'Brasileiros não precisam de visto para turismo de até 90 dias a cada 180 no espaço Schengen. Leve passaporte válido por 3 meses após a saída, comprovantes de hospedagem, recursos e passagem de volta, e seguro viagem.',
       NULL, 'https://home-affairs.ec.europa.eu/policies/schengen-borders-and-visa/visa-policy_en',
       'DRAFT', 'MANUAL', NOW(6), NOW(6)
FROM country WHERE schengen;

INSERT INTO destination_requirement
  (country_code, nationality, type, title, description, lead_days, official_url, status, source,
   created_at, updated_at)
SELECT code, 'BR', 'ETA', 'Autorização ETIAS (espaço Schengen)',
       'Autorização eletrônica obrigatória para isentos de visto quando o ETIAS entrar em vigor. Peça pelo site oficial com pelo menos 4 dias de antecedência.',
       4, 'https://travel-europe.europa.eu/etias_en', 'DRAFT', 'MANUAL', NOW(6), NOW(6)
FROM country WHERE schengen;

-- Mercosul e associados: entrada com RG.
INSERT INTO destination_requirement
  (country_code, nationality, type, title, description, lead_days, official_url, status, source,
   created_at, updated_at)
SELECT code, 'BR', 'ID_CARD', 'Entrada com RG ou passaporte',
       'Brasileiros podem entrar com o RG (carteira de identidade) em bom estado e emitido há menos de 10 anos, ou com passaporte válido. CNH e documentos de classe não valem. Menores precisam de autorização se viajarem sem os dois pais.',
       NULL, 'https://www.gov.br/mre/pt-br/assuntos/portal-consular', 'DRAFT', 'MANUAL', NOW(6), NOW(6)
FROM country WHERE br_id_card_accepted AND code <> 'BR';
