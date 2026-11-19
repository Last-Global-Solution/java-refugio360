package org.fiap.gs_final.config;

import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles({"prod", "demo"})
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-seed-guard;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=segredo-externo-apenas-para-teste-com-mais-de-32-bytes",
        "app.bootstrap-gestor.email=gestor-teste@example.org",
        "app.bootstrap-gestor.password=senha-de-teste-nao-publica",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "refugio360.seed.demo-enabled=true"
})
class ProdSeedGuardTest {
    @Autowired private LocalSeguroRepository locais;

    @Test
    void prodBloqueiaSeedMesmoComDemoEFlagAtivos() {
        assertEquals(0, locais.count());
    }
}
