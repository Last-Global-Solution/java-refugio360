package org.fiap.gs_final.config;

import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("demo")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-demo-disabled;DB_CLOSE_DELAY=-1",
        "app.jwt.secret=segredo-externo-apenas-para-teste-com-mais-de-32-bytes",
        "app.bootstrap-gestor.email=gestor-teste@example.org",
        "app.bootstrap-gestor.password=senha-de-teste-nao-publica",
        "refugio360.seed.demo-enabled=false"
})
class DemoProfileSeedDisabledTest {
    @Autowired private LocalSeguroRepository locais;

    @Test
    void demoSemFlagNaoInsereDados() {
        assertEquals(0, locais.count());
    }
}
