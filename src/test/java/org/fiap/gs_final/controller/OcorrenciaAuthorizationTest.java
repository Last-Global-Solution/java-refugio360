package org.fiap.gs_final.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiap.gs_final.model.Ocorrencia;
import org.fiap.gs_final.repository.OcorrenciaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ocorrencias-autorizacao;DB_CLOSE_DELAY=-1",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "refugio360.seed.demo-enabled=false"
})
@AutoConfigureMockMvc
class OcorrenciaAuthorizationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private OcorrenciaRepository ocorrencias;

    @Test
    void cadastroLoginEIsolamentoDeOcorrenciasEntreDoisCidadaos() throws Exception {
        String emailA = "cidadao-a-" + UUID.randomUUID() + "@example.org";
        String emailB = "cidadao-b-" + UUID.randomUUID() + "@example.org";
        String tokenA = cadastrar(emailA);
        String tokenB = cadastrar(emailB);
        String tokenGestor = login("gestor@refugio360.local", "gestor12345");

        long idA = criarOcorrencia(tokenA);
        long idB = criarOcorrencia(tokenB);
        long idLegado = ocorrencias.save(new Ocorrencia("Relato legado", "BAIXA", -23.55, -46.63)).getId();

        mvc.perform(get("/api/ocorrencias/{id}", idA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ocorrencias/{id}", idA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ocorrencias/{id}", idA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ocorrencias/{id}", Long.MAX_VALUE).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/ocorrencias/{id}", idB).header("Authorization", bearer(tokenGestor)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ocorrencias/{id}", idLegado).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ocorrencias/{id}", idLegado).header("Authorization", bearer(tokenGestor)))
                .andExpect(status().isOk());

        for (String rota : new String[]{"/api/ocorrencias", "/api/ocorrencias/minhas"}) {
            JsonNode pagina = pagina(rota, tokenA);
            assertTrue(contemId(pagina, idA));
            assertFalse(contemId(pagina, idB));
            assertFalse(contemId(pagina, idLegado));
        }
        JsonNode paginaGestor = pagina("/api/ocorrencias", tokenGestor);
        assertTrue(contemId(paginaGestor, idA));
        assertTrue(contemId(paginaGestor, idB));
        assertTrue(contemId(paginaGestor, idLegado));
        assertFalse(contemId(pagina("/api/ocorrencias/minhas", tokenB), idA));
        mvc.perform(get("/api/ocorrencias")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/ocorrencias/{id}/orientacoes", idLegado)
                        .header("Authorization", bearer(tokenA))).andExpect(status().isForbidden());
        mvc.perform(get("/api/ocorrencias/{id}/orientacoes", idLegado)
                        .header("Authorization", bearer(tokenGestor))).andExpect(status().isOk());
    }

    private String cadastrar(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Credenciais(email, "senha-forte-de-teste"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("accessToken").asText();
        assertFalse(token.isBlank());
        assertFalse(login(email, "senha-forte-de-teste").isBlank());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        return token;
    }

    private String login(String email, String senha) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Credenciais(email, senha))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }

    private long criarOcorrencia(String token) throws Exception {
        String response = mvc.perform(post("/api/ocorrencias").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Alagamento fictício\",\"urgencia\":\"BAIXA\",\"latitude\":-23.55,\"longitude\":-46.63}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private JsonNode pagina(String rota, String token) throws Exception {
        String response = mvc.perform(get(rota).param("size", "100").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private static boolean contemId(JsonNode pagina, long id) {
        for (JsonNode item : pagina.get("content")) {
            if (item.get("id").asLong() == id) return true;
        }
        return false;
    }

    private static String bearer(String token) { return "Bearer " + token; }

    private record Credenciais(String email, String senha) {}
}
