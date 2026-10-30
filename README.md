# Travel Planner

Planejador de viagens para família/grupo, pensado primeiro para o celular. Além de organizar a viagem (paradas, transporte, hospedagem, checklists), ele aponta **o que está faltando** — transfer, documentos, malas — e sugere como resolver.

- Especificação: [`docs/especificacao.md`](docs/especificacao.md)
- Plano de implementação: [`docs/plano-implementacao.md`](docs/plano-implementacao.md)

## Rodando localmente

Requisitos: Docker, Java 21 e Node 22.

```bash
cp .env.example .env      # ajuste se precisar
./dev.sh                  # MySQL (Docker) + backend (:8082) + frontend (http://localhost:5173)
```

Os comandos de backend, frontend e testes estão em [`CLAUDE.md`](CLAUDE.md).
