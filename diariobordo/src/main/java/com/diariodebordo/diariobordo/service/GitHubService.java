package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.GitHubCommitDTO;
import com.diariodebordo.diariobordo.dto.GitHubUserDTO;
import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.model.GithubCommit;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.GithubCommitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Integração com a API do GitHub (US-19a/19b/19c).
 *
 * Commits são buscados do repositório da equipe (Team.githubRepo,
 * formato "owner/repo") filtrados pelo usuário GitHub vinculado ao
 * perfil, e persistidos em GithubCommit como cache local — não usa
 * @Cacheable porque a US-19b exige mostrar dados cacheados com
 * timestamp real quando a API está limitada (403/429), o que precisa
 * sobreviver a restart e ser consultável, coisa que um cache em
 * memória não garante.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final int CACHE_FRESHNESS_MINUTES = 15;

    private final ObjectMapper objectMapper;
    private final GithubCommitRepository githubCommitRepository;

    public GitHubService(GithubCommitRepository githubCommitRepository) {
        this.objectMapper = new ObjectMapper();
        this.githubCommitRepository = githubCommitRepository;
    }

    public GitHubUserDTO getUserInfo(String githubUsername) {
        try {
            HttpResult result = sendGetRequest("https://api.github.com/users/" + githubUsername);
            if (result == null || result.statusCode != 200) return null;

            JsonNode json = objectMapper.readTree(result.body);
            if (!json.has("login")) return null;

            GitHubUserDTO user = new GitHubUserDTO();
            user.setLogin(json.path("login").asText());
            user.setName(json.path("name").asText());
            user.setAvatarUrl(json.path("avatar_url").asText());
            user.setBio(json.path("bio").asText());
            user.setPublicRepos(json.path("public_repos").asInt(0));
            user.setFollowers(json.path("followers").asInt(0));
            user.setFollowing(json.path("following").asInt(0));

            return user;
        } catch (Exception e) {
            log.error("Erro ao buscar perfil GitHub de {}: {}", githubUsername, e.getMessage());
            return null;
        }
    }

    public boolean validateGitHubUsername(String username) {
        try {
            HttpResult result = sendGetRequest("https://api.github.com/users/" + username);
            if (result == null || result.statusCode != 200) return false;
            JsonNode json = objectMapper.readTree(result.body);
            return json.has("login");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna os commits do usuário no repositório da equipe. Usa o
     * cache local (GithubCommit) se a última busca tiver menos de 15
     * minutos; caso contrário busca na API do GitHub e faz upsert dos
     * commits novos. Se a API responder com rate-limit (403/429) ou
     * qualquer outro erro, silenciosamente cai de volta pro que já
     * está em cache (mesmo que velho) em vez de mostrar vazio.
     */
    @Transactional
    public CommitFetchResult fetchCommits(User user, Team team) {
        if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
            return CommitFetchResult.semUsuarioVinculado();
        }
        if (team == null || team.getGithubRepo() == null || team.getGithubRepo().isBlank()) {
            return CommitFetchResult.semRepositorioConfigurado();
        }

        String repo = normalizeRepo(team.getGithubRepo());
        Optional<GithubCommit> maisRecente = githubCommitRepository.findFirstByUserOrderByFetchedAtDesc(user);
        boolean cacheFresco = maisRecente.isPresent()
                && maisRecente.get().getFetchedAt().isAfter(LocalDateTime.now().minusMinutes(CACHE_FRESHNESS_MINUTES));

        boolean rateLimited = false;

        if (!cacheFresco) {
            FetchOutcome outcome = fetchFromGitHubApi(user.getGithubUsername(), repo);
            if (outcome.rateLimited) {
                rateLimited = true;
                log.warn("GitHub API rate-limited ao buscar commits de {} em {}; usando cache", user.getGithubUsername(), repo);
            } else if (outcome.commits != null) {
                for (GithubCommit commit : outcome.commits) {
                    if (!githubCommitRepository.existsByUserAndSha(user, commit.getSha())) {
                        commit.setUser(user);
                        githubCommitRepository.save(commit);
                    }
                }
            }
        }

        List<GithubCommit> cached = githubCommitRepository.findByUserOrderByCommitDateDesc(user);
        List<GitHubCommitDTO> dtos = cached.stream()
                .map(c -> toDTO(c, repo))
                .collect(Collectors.toList());

        LocalDateTime lastSyncedAt = githubCommitRepository.findFirstByUserOrderByFetchedAtDesc(user)
                .map(GithubCommit::getFetchedAt)
                .orElse(null);

        return CommitFetchResult.ok(dtos, lastSyncedAt, rateLimited);
    }

    public List<HeatmapDataDTO> buildHeatmap(List<GitHubCommitDTO> commits, int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);

        if (commits.isEmpty()) {
            return generateEmptyHeatmap(startDate, today);
        }

        Map<LocalDate, Integer> commitCountByDate = new HashMap<>();
        Map<LocalDate, String> commitSummaryByDate = new HashMap<>();

        for (GitHubCommitDTO commit : commits) {
            if (commit.getDate() == null) continue;
            LocalDate date = commit.getDate().toLocalDate();
            if (!date.isBefore(startDate) && !date.isAfter(today)) {
                commitCountByDate.merge(date, 1, Integer::sum);
                String atual = commitSummaryByDate.getOrDefault(date, "");
                commitSummaryByDate.put(date, atual.isEmpty() ? commit.getMessage() : atual + " | " + commit.getMessage());
            }
        }

        List<HeatmapDataDTO> heatmapData = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            HeatmapDataDTO data = new HeatmapDataDTO();
            data.setDate(date);
            data.setCount(commitCountByDate.getOrDefault(date, 0));
            data.setSummary(commitSummaryByDate.getOrDefault(date, "Nenhum commit"));
            data.setType("commit");
            heatmapData.add(data);
        }
        return heatmapData;
    }

    private FetchOutcome fetchFromGitHubApi(String githubUsername, String repo) {
        try {
            String url = "https://api.github.com/repos/" + repo + "/commits?author=" + githubUsername + "&per_page=100";
            HttpResult result = sendGetRequest(url);

            if (result == null) {
                return FetchOutcome.erro();
            }
            if (result.statusCode == 403 || result.statusCode == 429) {
                return FetchOutcome.limitado();
            }
            if (result.statusCode != 200) {
                log.warn("GitHub API retornou {} para {}", result.statusCode, url);
                return FetchOutcome.erro();
            }

            JsonNode jsonArray = objectMapper.readTree(result.body);
            if (!jsonArray.isArray()) {
                return FetchOutcome.erro();
            }

            List<GithubCommit> commits = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            for (JsonNode node : jsonArray) {
                String sha = node.path("sha").asText(null);
                if (sha == null || sha.isBlank()) continue;

                JsonNode commitNode = node.path("commit");
                String message = commitNode.path("message").asText("");
                String dateStr = commitNode.path("author").path("date").asText(null);

                LocalDateTime commitDate;
                try {
                    commitDate = dateStr != null ? LocalDateTime.parse(dateStr, formatter) : LocalDateTime.now();
                } catch (Exception e) {
                    commitDate = LocalDateTime.now();
                }

                GithubCommit commit = new GithubCommit();
                commit.setSha(sha);
                commit.setMessage(firstLine(message));
                commit.setCommitDate(commitDate);
                commits.add(commit);
            }

            return FetchOutcome.sucesso(commits);
        } catch (Exception e) {
            log.error("Erro ao buscar commits de {} em {}: {}", githubUsername, repo, e.getMessage());
            return FetchOutcome.erro();
        }
    }

    private String firstLine(String message) {
        if (message == null) return "";
        int idx = message.indexOf('\n');
        return idx > 0 ? message.substring(0, idx) : message;
    }

    private GitHubCommitDTO toDTO(GithubCommit commit, String repo) {
        GitHubCommitDTO dto = new GitHubCommitDTO();
        dto.setSha(commit.getSha());
        dto.setMessage(commit.getMessage());
        dto.setDate(commit.getCommitDate());
        dto.setRepoName(repo);
        dto.setUrl("https://github.com/" + repo + "/commit/" + commit.getSha());
        return dto;
    }

    private String normalizeRepo(String repo) {
        String r = repo.trim();
        r = r.replaceFirst("^https?://github\\.com/", "");
        r = r.replaceFirst("\\.git$", "");
        r = r.replaceFirst("/$", "");
        return r;
    }

    private HttpResult sendGetRequest(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                return new HttpResult(responseCode, null);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new HttpResult(responseCode, response.toString());
        } catch (Exception e) {
            log.error("Erro de conexão com {}: {}", urlString, e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private List<HeatmapDataDTO> generateEmptyHeatmap(LocalDate startDate, LocalDate today) {
        List<HeatmapDataDTO> emptyData = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            HeatmapDataDTO data = new HeatmapDataDTO();
            data.setDate(date);
            data.setCount(0);
            data.setSummary("Nenhum commit");
            data.setType("none");
            emptyData.add(data);
        }
        return emptyData;
    }

    private static class HttpResult {
        final int statusCode;
        final String body;

        HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static class FetchOutcome {
        final List<GithubCommit> commits;
        final boolean rateLimited;

        private FetchOutcome(List<GithubCommit> commits, boolean rateLimited) {
            this.commits = commits;
            this.rateLimited = rateLimited;
        }

        static FetchOutcome sucesso(List<GithubCommit> commits) { return new FetchOutcome(commits, false); }
        static FetchOutcome limitado() { return new FetchOutcome(null, true); }
        static FetchOutcome erro() { return new FetchOutcome(null, false); }
    }

    public static class CommitFetchResult {
        private final List<GitHubCommitDTO> commits;
        private final LocalDateTime lastSyncedAt;
        private final boolean rateLimited;
        private final String status;

        private CommitFetchResult(List<GitHubCommitDTO> commits, LocalDateTime lastSyncedAt, boolean rateLimited, String status) {
            this.commits = commits;
            this.lastSyncedAt = lastSyncedAt;
            this.rateLimited = rateLimited;
            this.status = status;
        }

        public static CommitFetchResult ok(List<GitHubCommitDTO> commits, LocalDateTime lastSyncedAt, boolean rateLimited) {
            return new CommitFetchResult(commits, lastSyncedAt, rateLimited, "ok");
        }

        static CommitFetchResult semUsuarioVinculado() {
            return new CommitFetchResult(List.of(), null, false, "sem_usuario_vinculado");
        }

        static CommitFetchResult semRepositorioConfigurado() {
            return new CommitFetchResult(List.of(), null, false, "sem_repositorio_configurado");
        }

        public List<GitHubCommitDTO> getCommits() { return commits; }
        public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
        public boolean isRateLimited() { return rateLimited; }
        public boolean isSemRepositorioConfigurado() { return "sem_repositorio_configurado".equals(status); }
    }
}
