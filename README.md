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
| Backend | Java 17 + Spring Boot 3.5 |
| Segurança | Spring Security + BCrypt |
| Frontend | Thymeleaf |
| Banco de dados | PostgreSQL 15 |
| Build | Maven (wrapper incluído, não precisa instalar Maven) |
| Deploy | Docker / Railway |

---

## 🚀 Como rodar o projeto (qualquer máquina)

Existem duas formas de rodar. **A opção com Docker é a recomendada** — não exige Java, Maven nem PostgreSQL instalados na máquina, só o Docker.

### Opção A — Docker Compose (recomendado, um único comando)

**Pré-requisito:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e aberto (Windows, Mac ou Linux).

```bash
# 1. Extraia o .zip e entre na pasta do projeto
cd diario-de-bordo-de-equipes

# 2. Suba tudo (banco de dados + aplicação) com um comando
docker compose up --build
```

Aguarde a mensagem `Started DiariobordoApplication` no terminal (leva ~1 min na primeira vez, pois baixa as dependências e compila). Depois disso, acesse:

**http://localhost:8080/login**

Para parar tudo: `Ctrl+C` no terminal, depois `docker compose down` (ou `docker compose down -v` se quiser apagar também os dados do banco).

> Esse comando sobe dois containers: um PostgreSQL (`diariobordo-db`) e a aplicação Spring Boot (`diariobordo-app`), já conectados entre si — não precisa configurar nada manualmente.

### Opção B — Rodando localmente sem Docker

**Pré-requisitos:**

- [Java 17 (JDK)](https://adoptium.net/) ou superior
- [PostgreSQL](https://www.postgresql.org/download/) instalado e rodando localmente
- Maven **não é necessário instalar** — o projeto já vem com o Maven Wrapper (`mvnw` / `mvnw.cmd`)

**Passos:**

1. Crie o banco de dados no seu PostgreSQL local:
   ```sql
   CREATE DATABASE diariobordo;
   ```
   Por padrão o projeto espera usuário `postgres` e senha `postgres` (ver [application.properties](diariobordo/src/main/resources/application.properties)). Se o seu Postgres local usa outras credenciais, ajuste via variáveis de ambiente antes de rodar:
   ```bash
   # Linux/Mac
   export PGUSER=seu_usuario
   export PGPASSWORD=sua_senha

   # Windows (PowerShell)
   $env:PGUSER="seu_usuario"
   $env:PGPASSWORD="sua_senha"
   ```

2. Entre na pasta da aplicação e rode com o wrapper do Maven:
   ```bash
   cd diariobordo

   # Linux/Mac
   ./mvnw spring-boot:run

   # Windows
   mvnw.cmd spring-boot:run
   ```

3. Acesse **http://localhost:8080/login**

---

## 👤 Contas de teste

Na primeira execução, o sistema cria automaticamente 3 usuários (um por perfil) para facilitar os testes:

| Perfil | E-mail | Senha |
|---|---|---|
| Membro | `membro@email.com` | `123456` |
| Líder | `lider@email.com` | `123456` |
| Professora | `professora@email.com` | `123456` |

Também é possível criar uma conta nova pela tela de **Cadastro** (`/register`), escolhendo o perfil desejado.

---

## 🧪 Rodando os testes

```bash
cd diariobordo
./mvnw test          # Linux/Mac
mvnw.cmd test         # Windows
```

Os testes usam banco H2 em memória (não precisa do PostgreSQL rodando para eles).

> ⚠️ 4 de 71 testes (`DailyEntryControllerTest`, relacionados ao filtro de datas de `/member/history`) estão falhando atualmente — o controller foi refatorado para agrupar o histórico por sprint e não usa mais `startDate`/`endDate` no modelo, mas os testes antigos não foram atualizados para refletir isso. Não é um problema de ambiente/deploy.

---

## ⚙️ Variáveis de ambiente (referência)

A aplicação lê estas variáveis, todas com valor padrão para rodar localmente sem configurar nada:

| Variável | Padrão local | Descrição |
|---|---|---|
| `PGHOST` | `localhost` | Host do PostgreSQL |
| `PGPORT` | `5432` | Porta do PostgreSQL |
| `PGDATABASE` | `diariobordo` | Nome do banco |
| `PGUSER` | `postgres` | Usuário do banco |
| `PGPASSWORD` | `postgres` | Senha do banco |
| `PORT` | `8080` | Porta em que a aplicação sobe |
| `THYMELEAF_CACHE` | `false` | Cache de templates (deixe `true` só em produção) |

---

## 🩺 Problemas comuns

- **"port is already allocated" / porta 8080 ou 5432 em uso** — outro processo já está usando a porta. Feche o que estiver rodando nela, ou edite as portas em `docker-compose.yml` (ex: troque `"8080:8080"` por `"8081:8080"` e acesse via `:8081`).
- **Docker não conecta ("Cannot connect to the Docker daemon")** — abra o Docker Desktop e espere ele finalizar de iniciar antes de rodar o comando.
- **`./mvnw: Permission denied` (Linux/Mac, Opção B)** — rode `chmod +x mvnw` antes.
- **Erro de conexão com o banco (Opção B)** — confirme que o PostgreSQL local está rodando e que o banco `diariobordo` foi criado (`CREATE DATABASE diariobordo;`).

---

## 📁 Estrutura do projeto

```
diario-de-bordo-de-equipes/
├── docker-compose.yml          # sobe app + banco com um comando
└── diariobordo/                 # aplicação Spring Boot
    ├── Dockerfile
    ├── pom.xml
    ├── mvnw / mvnw.cmd           # Maven Wrapper
    └── src/main/java/.../
        ├── controller/           # rotas MVC
        ├── service/              # regras de negócio
        ├── repository/           # acesso a dados (Spring Data JPA)
        ├── model/                # entidades JPA
        └── dto/                  # objetos de formulário
```

---

## 🚀 Deploy (Railway)

O projeto já vem pronto para deploy no [Railway](https://railway.app) via Docker.

1. Crie um projeto novo no Railway e adicione um serviço **PostgreSQL** (Provision → Database → PostgreSQL).
2. Adicione um segundo serviço a partir deste repositório (Deploy from GitHub repo).
3. Nas configurações desse serviço, defina o **Root Directory** como `diariobordo` (é onde ficam o `Dockerfile`, o `pom.xml` e o código-fonte).
4. Em **Variables**, adicione (valores em texto puro, sem sintaxe de referência `${{ }}` — copie os valores reais direto da aba Variables do serviço Postgres):

   | Variável | Valor |
   |---|---|
   | `PGHOST` | `postgres.railway.internal` (ou o domínio privado do seu serviço Postgres) |
   | `PGPORT` | `5432` |
   | `PGDATABASE` | (copiar do serviço Postgres) |
   | `PGUSER` | (copiar do serviço Postgres) |
   | `PGPASSWORD` | (copiar do serviço Postgres) |
   | `THYMELEAF_CACHE` | `true` |

   O Railway já injeta `PORT` automaticamente — a aplicação lê essa variável sozinha.
5. Deploy. O Railway detecta o `diariobordo/Dockerfile` e o `railway.json` (build via Docker multi-stage, healthcheck em `/actuator/health`).
6. Em **Settings → Networking**, clique em "Generate Domain" e informe a porta `8080` quando solicitado.

> ⚠️ O `DataInitializer` cria automaticamente os 3 usuários de teste (ver seção "Contas de teste" acima) na primeira execução. Como o app fica público, troque essas senhas (ou remova o initializer) antes de divulgar o ambiente de produção.
