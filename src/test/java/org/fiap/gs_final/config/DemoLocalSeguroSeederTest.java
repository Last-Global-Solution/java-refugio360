package org.fiap.gs_final.config;

import org.fiap.gs_final.model.LocalSeguro;
import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class DemoLocalSeguroSeederTest {
    private static final int QUANTIDADE_DEMO = 6;
    private static final String NOME_EXISTENTE = "DEMO - Escola Municipal Abrigo Norte";

    @Autowired
    private LocalSeguroRepository repository;

    private DemoLocalSeguroSeeder seeder;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        seeder = new DemoLocalSeguroSeeder(repository);
    }

    @Test
    void seedHabilitadoInsereTodosOsLocaisEsperados() throws Exception {
        executarSeed();

        assertEquals(QUANTIDADE_DEMO, repository.count());
        assertTrue(repository.findAll().stream().allMatch(local -> local.getNome().startsWith("DEMO - ")));
    }

    @Test
    void segundaExecucaoNaoDuplicaRegistros() throws Exception {
        executarSeed();
        executarSeed();

        assertEquals(QUANTIDADE_DEMO, repository.count());
    }

    @Test
    void execucaoParcialInsereSomenteAusentesEPreservaRegistrosExistentes() throws Exception {
        LocalSeguro demoExistente = repository.saveAndFlush(new LocalSeguro(
                NOME_EXISTENTE,
                "TIPO_PERSONALIZADO",
                "Endereço preservado",
                -22.0,
                -45.0,
                "Descrição preservada"
        ));
        LocalSeguro real = repository.saveAndFlush(new LocalSeguro(
                "Local comunitário cadastrado pelo gestor",
                "ABRIGO",
                "Endereço real preservado",
                -23.0,
                -46.0,
                "Registro fora do seed"
        ));

        executarSeed();

        assertEquals(QUANTIDADE_DEMO + 1, repository.count());
        LocalSeguro demoDepois = buscarPorId(demoExistente.getId());
        assertEquals("TIPO_PERSONALIZADO", demoDepois.getTipo());
        assertEquals("Endereço preservado", demoDepois.getEndereco());
        assertEquals("Descrição preservada", demoDepois.getDescricao());
        LocalSeguro realDepois = buscarPorId(real.getId());
        assertEquals("Local comunitário cadastrado pelo gestor", realDepois.getNome());
        assertEquals("Endereço real preservado", realDepois.getEndereco());
    }

    @Test
    void dadosDemoPreenchemCamposObrigatoriosETemCoordenadasValidas() throws Exception {
        executarSeed();

        List<LocalSeguro> locais = repository.findAll();
        assertEquals(QUANTIDADE_DEMO, locais.size());
        for (LocalSeguro local : locais) {
            assertFalse(local.getNome().isBlank());
            assertTrue(local.getNome().startsWith("DEMO - "));
            assertFalse(local.getTipo().isBlank());
            assertFalse(local.getEndereco().isBlank());
            assertNotNull(local.getLatitude());
            assertTrue(local.getLatitude() >= -90 && local.getLatitude() <= 90);
            assertNotNull(local.getLongitude());
            assertTrue(local.getLongitude() >= -180 && local.getLongitude() <= 180);
            assertNotNull(local.getDescricao());
            assertTrue(local.getDescricao().toLowerCase().contains("fict"));
        }
    }

    private void executarSeed() throws Exception {
        seeder.run(new DefaultApplicationArguments(new String[0]));
    }

    private LocalSeguro buscarPorId(Long id) {
        return repository.findById(id).orElseThrow();
    }
}
