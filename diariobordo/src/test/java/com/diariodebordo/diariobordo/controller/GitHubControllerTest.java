package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.config.SecurityConfig;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.CustomUserDetailsService;
import com.diariodebordo.diariobordo.service.GitHubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GitHubController.class)
@Import(SecurityConfig.class)
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubService gitHubService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TeamRepository teamRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ─── GET /github/link ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getLinkForm_deveRetornarStatus200EViewLink() throws Exception {
        User user = criarUsuario("membro@test.com", null);
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/github/link"))
                .andExpect(status().isOk())
                .andExpect(view().name("github/link"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("currentGithub", (Object) null));
    }

    @Test
    void getLinkForm_semAutenticacao_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/github/link"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ─── POST /github/link ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void postLink_comUsernameValido_devePersistirEVincularERedirecionar() throws Exception {
        User user = criarUsuario("membro@test.com", null);
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(gitHubService.validateGitHubUsername("octocat")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/github/link")
                        .with(csrf())
                        .param("githubUsername", "octocat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/github/link"))
                .andExpect(flash().attributeExists("success"));

        // garante que o username foi de fato persistido no usuário, não só o redirect ocorreu
        verify(userRepository).save(argThat(u -> "octocat".equals(u.getGithubUsername())));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void postLink_comUsernameInvalido_naoDevePersistirEDeveAdicionarErro() throws Exception {
        User user = criarUsuario("membro@test.com", null);
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(gitHubService.validateGitHubUsername("usuario-inexistente-xpto")).thenReturn(false);

        mockMvc.perform(post("/github/link")
                        .with(csrf())
                        .param("githubUsername", "usuario-inexistente-xpto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/github/link"))
                .andExpect(flash().attribute("error", "Usuário do GitHub não encontrado."));

        verify(userRepository, never()).save(any(User.class));
    }

    // ─── POST /github/unlink ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void postUnlink_deveLimparUsernameEDesvincular() throws Exception {
        User user = criarUsuario("membro@test.com", "octocat");
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/github/unlink")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/github/link"))
                .andExpect(flash().attributeExists("success"));

        verify(userRepository).save(argThat(u -> u.getGithubUsername() == null));
    }

    // ─── GET /github/heatmap ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHeatmap_semGithubVinculado_deveRedirecionarParaLink() throws Exception {
        User user = criarUsuario("membro@test.com", null);
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/github/heatmap"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/github/link"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHeatmap_semRepositorioConfigurado_deveRedirecionarComErro() throws Exception {
        User user = criarUsuario("membro@test.com", "octocat");
        Team teamSemRepo = new Team();
        teamSemRepo.setId(1L);
        teamSemRepo.setGithubRepo(null);
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(teamRepository.findByMemberId(1L)).thenReturn(List.of(teamSemRepo));

        mockMvc.perform(get("/github/heatmap"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/github/link"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHeatmap_comRepositorioConfigurado_deveRenderizarComDadosDoService() throws Exception {
        User user = criarUsuario("membro@test.com", "octocat");
        Team team = new Team();
        team.setId(1L);
        team.setGithubRepo("jsminemra/diario-de-bordo-de-equipes");
        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(teamRepository.findByMemberId(1L)).thenReturn(List.of(team));
        when(gitHubService.fetchCommits(eq(user), eq(team)))
                .thenReturn(GitHubService.CommitFetchResult.ok(List.of(), null, false));
        when(gitHubService.buildHeatmap(any(), eq(180))).thenReturn(List.of());

        mockMvc.perform(get("/github/heatmap"))
                .andExpect(status().isOk())
                .andExpect(view().name("github/heatmap"))
                .andExpect(model().attribute("githubRepo", "jsminemra/diario-de-bordo-de-equipes"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User criarUsuario(String email, String githubUsername) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setName("Membro Teste");
        u.setRole(User.Role.MEMBER);
        u.setGithubUsername(githubUsername);
        return u;
    }
}
