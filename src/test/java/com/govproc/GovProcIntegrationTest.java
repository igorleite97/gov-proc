package com.govproc;

import com.fasterxml.jackson.databind.JsonNode;
import com.govproc.auth.dto.LoginRequest;
import com.govproc.auth.dto.RegisterRequest;
import com.govproc.process.dto.CaptureProcessRequest;
import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração reais: Spring Boot completo + PostgreSQL em container
 * (Testcontainers) + Flyway aplicando o schema de verdade.
 *
 * Cobrem o que os testes unitários (com mocks) não alcançam: as migrations
 * batendo contra um Postgres real, o mapeamento JPA validado (ddl-auto=validate),
 * a cadeia de segurança JWT e as constraints do banco.
 *
 * Um único container compartilhado pelos casos desta classe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GovProcIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    @Test
    void todasAsMigrationsFlywayDevemAplicarNoPostgresReal() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        // V1..V11
        assertThat(applied).isEqualTo(11);
    }

    @Test
    void deveRegistrarLogarECapturarProcesso_persistindoNoBanco() {
        String email = "user-" + UUID.randomUUID() + "@govproc.test";

        // 1. Registro (endpoint público) — já retorna um JWT
        ResponseEntity<JsonNode> register = rest.postForEntity(
                "/auth/register", new RegisterRequest("Igor Andrade", email, "password123"), JsonNode.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Login → token
        ResponseEntity<JsonNode> login = rest.postForEntity(
                "/auth/login", new LoginRequest(email, "password123"), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().get("data").get("token").asText();
        assertThat(token).isNotBlank();

        // 3. Captura de processo (rota protegida — exige JWT)
        String processNumber = "PRC-" + UUID.randomUUID();
        ResponseEntity<JsonNode> capture = rest.exchange(
                "/processes", org.springframework.http.HttpMethod.POST,
                authEntity(captureRequest(processNumber), token), JsonNode.class);
        assertThat(capture.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String id = capture.getBody().get("data").get("id").asText();
        assertThat(id).isNotBlank();

        // 4. Consulta — confirma persistência no Postgres real com status inicial
        ResponseEntity<JsonNode> fetched = rest.exchange(
                "/processes/" + id, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("data").get("status").asText()).isEqualTo("CAPTURED");
    }

    @Test
    void deveRetornar409_quandoProcessoDuplicado_violandoConstraintDoBanco() {
        String email = "user-" + UUID.randomUUID() + "@govproc.test";
        rest.postForEntity("/auth/register", new RegisterRequest("Dup", email, "password123"), JsonNode.class);
        String token = rest.postForEntity("/auth/login", new LoginRequest(email, "password123"), JsonNode.class)
                .getBody().get("data").get("token").asText();

        String processNumber = "DUP-" + UUID.randomUUID();
        CaptureProcessRequest request = captureRequest(processNumber);

        ResponseEntity<JsonNode> first = rest.exchange(
                "/processes", org.springframework.http.HttpMethod.POST, authEntity(request, token), JsonNode.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // mesmo processNumber + uasg → viola uq_process_number_uasg → 409 (BusinessException)
        ResponseEntity<JsonNode> second = rest.exchange(
                "/processes", org.springframework.http.HttpMethod.POST, authEntity(request, token), JsonNode.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // -------------------------------------------------------------------------

    private CaptureProcessRequest captureRequest(String processNumber) {
        return new CaptureProcessRequest(
                processNumber, "654321", "Órgão de Teste", "Compras.gov.br",
                "Aquisição de equipamentos de TI", null, null, null,
                PriorityLevel.HIGH, RiskLevel.LOW);
    }

    private HttpEntity<Object> authEntity(Object body, String token) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
