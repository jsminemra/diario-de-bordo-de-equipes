# 📓 Diário de Bordo de Equipe
 
> Sistema web para registro diário de stand-ups assíncronos em equipes ágeis, com geração automática de relatórios de sprint.
 
---
 
## 💡 O que é
 
O **Diário de Bordo de Equipe** centraliza os registros diários de stand-up de equipes que trabalham com metodologia ágil. Cada membro responde três perguntas por dia — o que fez, o que fará e quais são seus impedimentos — e ao encerrar a sprint, o sistema gera o relatório automaticamente, sem trabalho manual de compilação.
 
Diferente de ferramentas como Jira ou Azure DevOps, o foco aqui é no **estado humano do projeto**: o que cada pessoa fez, sente e precisa, transformado em documentação de sprint sem esforço adicional.
 
---
 
## ✨ Funcionalidades
 
- **Autenticação e cadastro** — login individual com perfis distintos (membro, líder, professora)
- **Registro diário** — formulário com três campos: o que fiz, o que farei e impedimentos. Um registro por membro por dia
- **Feed da equipe** — histórico de registros da equipe organizados por sprint
- **Gestão de sprints** — criar, abrir e encerrar sprints com datas definidas e controle de acesso por perfil
- **Relatório automático** — gerado ao encerrar a sprint, agregando todos os registros diários da equipe
- **Painel da professora** — visão consolidada de todas as equipes com progresso de registro do dia
- **Alertas de ausência** — destaque visual para membros que não registraram no dia
- **Heatmap de commits** — integração com GitHub API para acompanhamento técnico por membro
- **Exportação PDF** — download do relatório de sprint com layout fiel à visualização web
---
 
## 🛠️ Tecnologias
 
| Camada | Tecnologia |
|---|---|
| Backend | Java 17 + Spring Boot |
| Segurança | Spring Security + BCrypt |
| Frontend | Thymeleaf |
| Banco de dados | PostgreSQL |
| Deploy | Railway / Render |
 
---
 
### Pré-requisitos
 
- Java 17+
- PostgreSQL rodando localmente
- Maven

---

## 🚀 Deploy (Railway)

O projeto já vem pronto para deploy no [Railway](https://railway.app) via Docker.

1. Crie um projeto novo no Railway e adicione um serviço **PostgreSQL** (Provision → Database → PostgreSQL).
2. Adicione um segundo serviço a partir deste repositório (Deploy from GitHub repo).
3. Nas configurações desse serviço, defina o **Root Directory** como `diariobordo` (é onde ficam o `Dockerfile`, o `pom.xml` e o código-fonte).
4. Em **Variables**, referencie as credenciais do banco Postgres provisionado (Railway permite `${{Postgres.PGHOST}}` etc.):

   | Variável | Valor |
   |---|---|
   | `PGHOST` | `${{Postgres.PGHOST}}` |
   | `PGPORT` | `${{Postgres.PGPORT}}` |
   | `PGDATABASE` | `${{Postgres.PGDATABASE}}` |
   | `PGUSER` | `${{Postgres.PGUSER}}` |
   | `PGPASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `THYMELEAF_CACHE` | `true` |

   O Railway já injeta `PORT` automaticamente — a aplicação lê essa variável sozinha.
5. Deploy. O Railway detecta o `diariobordo/Dockerfile` e o `railway.json` (build via Docker multi-stage, healthcheck em `/actuator/health`).

> ⚠️ O `DataInitializer` cria automaticamente 3 usuários de teste (`membro@email.com`, `lider@email.com`, `professora@email.com`, senha `123456`) na primeira execução. Como o app fica público, troque essas senhas (ou remova o initializer) antes de expor o ambiente de produção.
