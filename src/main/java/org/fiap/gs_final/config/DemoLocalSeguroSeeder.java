package org.fiap.gs_final.config;

import org.fiap.gs_final.model.LocalSeguro;
import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("!prod & (dev | demo)")
@ConditionalOnProperty(prefix = "refugio360.seed", name = "demo-enabled", havingValue = "true")
public class DemoLocalSeguroSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoLocalSeguroSeeder.class);

    private static final List<DemoLocal> LOCAIS = List.of(
            new DemoLocal(
                    "DEMO - Escola Municipal Abrigo Norte",
                    "ABRIGO",
                    "Rua Fictícia do Norte, 100 - São Paulo/SP",
                    -23.52180,
                    -46.63190,
                    "Dado fictício de demonstração. Não representa um abrigo oficial ou disponibilidade real."
            ),
            new DemoLocal(
                    "DEMO - Centro Comunitário Esperança",
                    "CENTRO_COMUNITARIO",
                    "Avenida Ilustrativa Esperança, 250 - São Paulo/SP",
                    -23.53540,
                    -46.65020,
                    "Centro comunitário fictício para demonstração do sistema; não possui atendimento real."
            ),
            new DemoLocal(
                    "DEMO - Ginásio de Apoio Central",
                    "GINASIO",
                    "Praça de Demonstração Central, 45 - São Paulo/SP",
                    -23.55050,
                    -46.63330,
                    "Ponto de apoio fictício. Confirme sempre locais oficiais antes de se deslocar."
            ),
            new DemoLocal(
                    "DEMO - Unidade de Apoio Comunitário Sul",
                    "UNIDADE_APOIO",
                    "Rua Exemplo da Zona Sul, 320 - São Paulo/SP",
                    -23.58010,
                    -46.64070,
                    "Unidade fictícia criada exclusivamente para demonstração e testes locais."
            ),
            new DemoLocal(
                    "DEMO - Ponto de Acolhimento Jardim",
                    "PONTO_ACOLHIMENTO",
                    "Alameda Fictícia do Jardim, 80 - São Paulo/SP",
                    -23.56420,
                    -46.62090,
                    "Ponto de acolhimento fictício, sem garantia de funcionamento ou segurança real."
            ),
            new DemoLocal(
                    "DEMO - Centro de Atendimento Temporário",
                    "ATENDIMENTO_TEMPORARIO",
                    "Avenida Cenário de Teste, 500 - São Paulo/SP",
                    -23.54260,
                    -46.61040,
                    "Local temporário fictício para validar o aplicativo; não é um serviço oficial."
            )
    );

    private final LocalSeguroRepository repository;

    public DemoLocalSeguroSeeder(LocalSeguroRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int inseridos = 0;
        for (DemoLocal local : LOCAIS) {
            if (repository.existsByNomeIgnoreCase(local.nome())) continue;
            repository.save(local.toEntity());
            inseridos++;
        }
        log.info("Seed DEMO de locais seguros concluído: {} inserido(s), {} já existente(s)",
                inseridos, LOCAIS.size() - inseridos);
    }

    private record DemoLocal(
            String nome,
            String tipo,
            String endereco,
            Double latitude,
            Double longitude,
            String descricao
    ) {
        LocalSeguro toEntity() {
            return new LocalSeguro(nome, tipo, endereco, latitude, longitude, descricao);
        }
    }
}
