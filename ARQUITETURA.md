# Como o sistema funciona

Visão geral do funcionamento do **Diário de Bordo de Equipes** — parte lógica (fluxos e regras de negócio) e parte técnica (Spring Boot, onde cada coisa está).

---

## 1. Arquitetura: Spring Boot monolítico, em camadas

É um app Spring Boot 3 + Java 17 clássico, **MVC com renderização no servidor** (não é uma API separada + frontend em React/Vue — o próprio Spring gera o HTML final via Thymeleaf e manda pronto pro navegador). Fluxo de uma requisição:

```
Navegador → Controller → Service → Repository → Banco (Postgres)
                ↓
           Model (dados) + nome da view
                ↓
         Thymeleaf renderiza o .html
                ↓
           HTML pronto volta pro navegador
```

Não tem separação de API REST + SPA — cada clique no menu é uma navegação de página de verdade (com exceção de pequenos trechos de JS pra coisas como o polling do contador de presença ou o clipboard do código de convite, que usam `fetch()` isolado).

---

## 2. Onde fica cada coisa (mapa de pastas)

```
src/main/java/com/diariodebordo/diariobordo/
├── controller/     → recebe a requisição HTTP, decide o que fazer, devolve a view
├── service/        → regra de negócio de verdade (onde a lógica "pesada" mora)
├── repository/     → interfaces do Spring Data JPA, uma por entidade (viram SQL sozinhas)
├── model/          → as entidades JPA (as tabelas do banco, uma classe = uma tabela)
├── dto/            → objetos simples só pra carregar dado entre formulário ↔ controller
└── config/         → configuração do Spring (segurança, dados iniciais de teste)

src/main/resources/
├── templates/      → os .html (Thymeleaf), organizados por área (leader/, member/, professor/, github/)
├── application.properties → configuração (banco, porta, etc.)
└── static/         → JS/CSS que não passa pelo Thymeleaf
```

### Controllers (`controller/`)

Um por área do sistema:

| Controller | Responsabilidade |
|---|---|
| `AuthController` / `RegisterController` | Login e cadastro |
| `DashboardController` | Decide pra onde te manda depois do login, dependendo do seu perfil |
| `TeamController` | Tudo do líder: criar equipe, sprint, adicionar membro, encerrar sprint, ver relatório, exportar PDF, código de convite, repositório GitHub da equipe |
| `MemberController` | Área do membro: feed, "sem equipe", entrar por código |
| `ProfessorController` | Painel consolidado, ver qualquer equipe, excluir equipe |
| `DailyEntryController` | Registro diário (o "o que fiz / o que farei / impedimentos") |
| `HeatmapController` | Heatmap de registros diários (US-08) |
| `GitHubController` | Vincular conta, heatmap de commits, lista de commits (US-19) |

### Services (`service/`)

Cada controller chama um ou mais desses. É aqui que ficam as regras tipo "só o líder pode X", "não pode ter duas sprints ativas", "gera código de 6 caracteres sem 0/O/1/I", etc.

### Repositories (`repository/`)

Interfaces vazias que estendem `JpaRepository<Entidade, Long>`. Você só declara a assinatura do método (`findByEmail(String email)`) e o Spring Data gera a query sozinho pelo nome do método. Nenhum SQL escrito à mão, exceto quando tem `@Query` explícito (poucos casos, tipo `findByMemberId`).

---

## 3. O modelo de dados (as entidades)

```
User (1) ──lidera──> (N) Team
User (N) <──membro de──> (N) Team   [tabela intermediária team_members]
Team (1) ──tem──> (N) Sprint
Sprint (1) ──tem──> (N) DailyEntry
Sprint (1) ──tem──> (1) SprintReport
User (1) ──autor de──> (N) DailyEntry
User (1) ──tem──> (N) GithubCommit  [cache local dos commits buscados na API do GitHub]
```

- **`User`**: email, senha (sempre BCrypt — validação no `@PrePersist`/`@PreUpdate` que rejeita salvar senha em texto puro), `role` (enum `MEMBER`/`LEADER`/`PROFESSOR`), `githubUsername`.
- **`Team`**: nome, líder, lista de membros, `code` (código de convite), `githubRepo` (owner/repo do GitHub da equipe).
- **`Sprint`**: nome, datas, status (`ATIVA`/`ENCERRADA`), pertence a uma Team.
- **`DailyEntry`**: o registro do dia — o que fiz, o que farei, impedimentos, pertence a um User e a uma Sprint.
- **`SprintReport`**: o resumo gerado quando a sprint encerra (total de registros, membros com registro etc.) — gerado **assincronamente**.
- **`GithubCommit`**: um commit real cacheado do GitHub (sha, data, mensagem).

---

## 4. Segurança (Spring Security)

Tudo centralizado em `config/SecurityConfig.java`. A regra é simples e por prefixo de URL:

```java
.requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
.requestMatchers("/professor/**").hasRole("PROFESSOR")
.requestMatchers("/leader/**").hasRole("LEADER")
.requestMatchers("/member/**").hasRole("MEMBER")
```

Ou seja: **o prefixo da URL já garante o controle de acesso** — não precisa checar role dentro de cada método. Senha checada via `BCryptPasswordEncoder`, autenticação via formulário padrão do Spring Security (`/login` faz `POST` pra ele mesmo, ele decide sucesso/falha).

**Autorização fina** (tipo "esse líder pode ver ESSA equipe específica, não qualquer uma") não é o Spring Security que faz — isso é checado manualmente dentro dos métodos, comparando `team.getLeader().getId().equals(leader.getId())`. Foi justamente a ausência desse tipo de checagem em alguns endpoints que causou bugs de IDOR já corrigidos no projeto.

---

## 5. Fluxos principais, passo a passo

**Login/Cadastro** → `RegisterController` salva o usuário com senha já criptografada, autentica na hora e salva a sessão explicitamente (`SecurityContextRepository.saveContext`) — sem isso a sessão não persistia e a primeira ação após cadastro jogava de volta pro login. Depois disso, toda requisição passa por `AuthController./dashboard`, que olha o `role` do usuário e redireciona pra área certa.

**Sem equipe** → se você é `MEMBER` e não tem equipe, cai em `member/no-team.html`, onde pode digitar o código de convite (`TeamService.joinTeamByCode`) ou esperar o líder te adicionar por e-mail.

**Registro diário** → `DailyEntryController` salva um `DailyEntry` vinculado à sprint ativa da sua equipe. Só pode ter um por dia por pessoa.

**Encerrar sprint** → `TeamController.closeSprint` muda o status pra `ENCERRADA` e dispara `SprintReportService.generateAndSaveAsync(sprintId)` — isso roda **em outra thread** (`@Async`), pra não travar a resposta HTTP esperando o relatório ser calculado. O método recebe só o `id` da sprint (não o objeto inteiro) porque uma thread separada não tem acesso à mesma sessão do banco da requisição original — ela busca tudo de novo do zero dentro da própria transação.

**Ver relatório** → `report.html` (visualização normal, com botão "Imprimir") ou exportação real em PDF via `PdfExportService`, que usa a biblioteca **OpenHTMLtoPDF** pra converter um template Thymeleaf separado (`report-pdf.html`, HTML mais simples e "achatado", sem grid/flexbox) direto em bytes de PDF.

**Excluir equipe** (professora) → `TeamService.deleteTeam` — apaga registros → relatórios → sprints → equipe, nessa ordem, numa transação só.

**GitHub** → `GitHubService` chama a API real do GitHub (`java.net.HttpURLConnection`, sem biblioteca de cliente HTTP) pra validar o usuário e buscar commits do repositório da equipe. Os commits ficam guardados na tabela `github_commits` como cache — se a última busca foi há menos de 15 minutos, nem chama a API de novo, só lê do banco.

---

## 6. Frontend: Thymeleaf + design system próprio, sem framework JS

Não tem React/Vue/Angular. Cada tela é um `.html` com atributos `th:*` (Thymeleaf) que o servidor processa antes de mandar pro navegador. Todo template puxa um cabeçalho comum:

```html
<head th:replace="~{fragments/head :: commonHead('Título')}"></head>
```

Isso importa a fonte (`Plus Jakarta Sans`) e um `styles.css` compartilhado com as variáveis visuais (`--bg`, `--border`, `--success` etc.) e classes reutilizáveis (`.card`, `.btn-primary`, `.badge`, `.input-group`...). É esse padrão visual que deve ser seguido em qualquer tela nova — sem Bootstrap ou Font Awesome, que não fazem parte do projeto.

JS puro (sem biblioteca) só onde precisa de interatividade sem recarregar página: o polling do contador de presença a cada 15s, o botão de copiar código, o tooltip do heatmap.

---

## 7. Banco de dados

Postgres real (não é H2 nem SQLite). `spring.jpa.hibernate.ddl-auto=update` — o Hibernate cria/ajusta as tabelas sozinho a partir das entidades Java, sem migração manual tipo Flyway. É prático pra esse estágio do projeto, mas exige cuidado ao adicionar colunas novas (sempre como *nullable*), pra não quebrar em cima de dados que já existiam.

**Testes** usam H2 em memória (banco fake, zerado a cada execução) — por isso os testes rodam rápido e isolados. Testes de integração com volume real de dados são melhor verificados contra o Postgres de verdade.

---

## 8. Testes

JUnit 5 + Mockito. Duas categorias:

- **Testes de controller** (`@WebMvcTest`) — sobem só a camada web, com os services mockados (`@MockitoBean`), simulam requisições HTTP de verdade via `MockMvc`.
- **Testes de service/repository** — testam a lógica ou o banco H2 isoladamente.
