package com.iyanc.javarush.readsprinterback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyanc.javarush.readsprinterback.dto.request.ExerciseResultRequest;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseResultResponse;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseSummaryResponse;
import com.iyanc.javarush.readsprinterback.security.JwtUtil;
import com.iyanc.javarush.readsprinterback.security.UserDetailsServiceImpl;
import com.iyanc.javarush.readsprinterback.service.ExerciseResultService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration-style MockMvc tests for {@link ExerciseResultController} (cases 10.1–10.5).
 */
@WebMvcTest(ExerciseResultController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=*")
@DisplayName("ExerciseResultController (MockMvc)")
class ExerciseResultControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ExerciseResultService resultService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final String BASE_URL      = "/api/results";
    private static final String ME_URL        = "/api/results/me";
    private static final String SUMMARY_URL   = "/api/results/me/summary";
    private static final String BY_TYPE_URL   = "/api/results/me/type/SPEED_READING";
    private static final String USER_EMAIL    = "test@example.com";

    private ExerciseResultRequest validRequest() {
        ExerciseResultRequest req = new ExerciseResultRequest();
        req.setExerciseType("SPEED_READING");
        req.setScore(90);
        req.setDurationSec(60);
        req.setWpm(300);
        req.setCorrectCount(9);
        req.setTotalCount(10);
        return req;
    }

    private ExerciseResultResponse sampleResponse() {
        return ExerciseResultResponse.builder()
                .id(1L)
                .exerciseType("SPEED_READING")
                .score(90)
                .wpm(300)
                .completedAt(LocalDateTime.now())
                .build();
    }

    // ─── 10.1 POST /api/results — авторизований, валідне тіло → 201 ──────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("10.1 POST /api/results — авторизований, валідне тіло → статус 201")
    void saveResult_authenticated_returns201() throws Exception {
        when(resultService.save(eq(USER_EMAIL), any())).thenReturn(sampleResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseType").value("SPEED_READING"));
    }

    // ─── 10.2 POST /api/results — без авторизації → 401 ─────────────────────

    @Test
    @DisplayName("10.2 POST /api/results — без авторизації → статус 401")
    void saveResult_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ─── 10.3 GET /api/results/me — авторизований → 200, масив ──────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("10.3 GET /api/results/me — авторизований → статус 200, масив результатів")
    void getMyResults_authenticated_returns200WithList() throws Exception {
        when(resultService.getMyResults(USER_EMAIL))
                .thenReturn(List.of(sampleResponse(), sampleResponse()));

        mockMvc.perform(get(ME_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].exerciseType").value("SPEED_READING"));
    }

    // ─── 10.4 GET /api/results/me/summary — авторизований → 200, summary ─────

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("10.4 GET /api/results/me/summary — авторизований → статус 200, масив ExerciseSummaryResponse")
    void getMySummary_authenticated_returns200WithSummary() throws Exception {
        ExerciseSummaryResponse summary = ExerciseSummaryResponse.builder()
                .exerciseType("SPEED_READING")
                .totalCount(5L)
                .avgScore(88.0)
                .avgWpm(280.0)
                .avgDurationSec(55.0)
                .build();
        when(resultService.getMySummary(USER_EMAIL)).thenReturn(List.of(summary));

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].exerciseType").value("SPEED_READING"))
                .andExpect(jsonPath("$[0].totalCount").value(5));
    }

    // ─── 10.5 GET /api/results/me/type/{type} — авторизований → 200, відфільтрований масив

    @Test
    @WithMockUser(username = USER_EMAIL)
    @DisplayName("10.5 GET /api/results/me/type/{exerciseType} — авторизований → статус 200, відфільтрований масив")
    void getMyResultsByType_authenticated_returns200Filtered() throws Exception {
        when(resultService.getMyResultsByType(USER_EMAIL, "SPEED_READING"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(BY_TYPE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].exerciseType").value("SPEED_READING"));
    }
}

