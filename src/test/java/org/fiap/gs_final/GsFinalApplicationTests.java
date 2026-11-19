package org.fiap.gs_final;

import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

// O teste de contexto não depende de um RabbitMQ no ar: o consumidor da fila não é iniciado.
@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.datasource.url=jdbc:h2:mem:refugio360-teste;DB_CLOSE_DELAY=-1",
		"refugio360.seed.demo-enabled=false"
})
class GsFinalApplicationTests {
	@Autowired
	private LocalSeguroRepository localSeguroRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void seedDemoDesabilitadoNaoInsereLocais() {
		assertEquals(0, localSeguroRepository.count());
	}

}
