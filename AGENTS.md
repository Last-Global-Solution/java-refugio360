# Convenções da API Java Refúgio 360

## Escopo

- Este repositório contém a API Java Spring Boot.
- Preserve alterações não commitadas e confirme o diff antes de editar arquivos já modificados.
- Não altere Mobile ou .NET a partir deste repositório.
- Não invente campos, enums ou endpoints; confira entidades, DTOs e regras existentes.

## Persistência

- Evite alterações de schema quando uma consulta derivada do Spring Data resolver o requisito.
- Seeds devem ser aditivos: nunca apagar, atualizar ou substituir registros existentes.
- O seed DEMO de locais usa o nome estável como identidade lógica e verifica cada registro separadamente.
- Nunca use `repository.count() == 0` para decidir a execução de seeds.

## Seed DEMO

- `DemoLocalSeguroSeeder` só pode existir nos profiles `dev` ou `demo`, sem `prod` ativo.
- A propriedade `refugio360.seed.demo-enabled` também deve estar explicitamente habilitada.
- O valor padrão deve permanecer `false`; produção deve permanecer protegida pelo profile e pela propriedade.
- Todo registro fictício deve começar com `DEMO -` e declarar na descrição que não representa serviço oficial.
- Não crie usuários, credenciais ou permissões como efeito do seed de locais.

## Segurança

- Não enfraqueça autenticação, autorização, CORS ou ownership para facilitar testes.
- Não versione secrets, bancos locais, tokens ou credenciais de ambientes compartilhados.
- Use H2 em memória e dados descartáveis nas validações automatizadas.
- No profile `demo` hospedado, exigir `JWT_SECRET`, `GESTOR_EMAIL` e `GESTOR_PASSWORD` externos; manter o console H2 desabilitado.
- Cidadãos só podem listar e consultar as próprias ocorrências; gestor pode consultar todas, inclusive as legadas sem autor. Aplique ownership no serviço e teste com dois usuários.

## Qualidade

- Execute `mvn test` antes de concluir alterações.
- Cubra seed desabilitado, primeira execução, repetição, carga parcial e preservação de dados existentes.
- Valide campos obrigatórios, coordenadas e identificação explícita dos dados DEMO.
