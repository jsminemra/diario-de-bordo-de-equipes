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
