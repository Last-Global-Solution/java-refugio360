package org.fiap.gs_final.config;

import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("demo")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-demo;DB_CLOSE_DELAY=-1",
        "app.jwt.secret=segredo-externo-apenas-para-teste-com-mais-de-32-bytes",
        "app.bootstrap-gestor.email=gestor-teste@example.org",
        "app.bootstrap-gestor.password=senha-de-teste-nao-publica",
        "refugio360.seed.demo-enabled=true"
})
class DemoProfileTest {
    @Autowired private LocalSeguroRepository locais;
    @Autowired private Environment environment;

    @Test
    void profileDemoProtegidoHabilitaSeedQuandoExplicitamenteSolicitado() {
        assertEquals("false", environment.getProperty("spring.h2.console.enabled"));
        assertEquals(6, locais.count());
        assertTrue(locais.findAll().stream().allMatch(local -> local.getNome().startsWith("DEMO - ")));
    }
}
