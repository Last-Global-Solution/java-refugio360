# Refúgio 360 — API

API REST do **Refúgio 360**, solução CitizenTech para apoiar comunidades durante ondas de calor, enchentes e falta de energia. O serviço cadastra locais seguros, recebe ocorrências, mapeia pontos de risco e gera orientações contextualizadas para cidadãos e gestores.

## Links:
| Tipo | Endereço |
|---|---|
| Swagger UI on Render | https://java-refugio360.onrender.com/swagger-ui/index.html |
| Deploy on Render (web service) | https://java-refugio360.onrender.com |
| Vídeo Pitch | https://youtu.be/-IpDw2jfjxQ?is=HDPhgcPD1SHWVyHX |

## Como os dados e a autenticação funcionam

O aplicativo Mobile usa cadastro e login locais da API Java e recebe um JWT local. A integração Firebase Authentication é opcional: quando configurada, a API também valida o ID token do Firebase. O H2 mantém usuários e dados do domínio.

| Responsabilidade | Onde fica |
|---|---|
| Cadastro e login no aplicativo | API Java (`/api/auth/register` e `/api/auth/login`) |
| Validação do JWT local e perfil `CIDADAO`/`GESTOR` | API Java com Spring Security; Firebase opcional |
| Locais seguros, ocorrências e pontos de risco | API Java + H2 |
| Orientações personalizadas | API Java + Spring AI/Gemini (com alternativa local) |
| Geração assíncrona das orientações | RabbitMQ (evento `OcorrenciaRegistrada`) |

**Repositório Mobile (app que consome esta API):** _https://github.com/Last-Global-Solution/mobile-refugio360.git_

O Mobile armazena apenas o JWT local via SecureStore, não a senha; envia `Authorization: Bearer <token>` nas rotas protegidas.

## Executar localmente

Requisitos: **JDK 21** e **Docker** (para o RabbitMQ).

1. Suba o RabbitMQ:

```bash
docker compose up -d
```

2. Inicie a API.

Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Sem o RabbitMQ a API também sobe: as ocorrências são registradas normalmente, só a orientação automática não é gerada (ela continua disponível sob demanda em `POST /api/ai/ocorrencias/{id}/orientacao`).

Se o Java 21 não estiver como padrão, ajuste `JAVA_HOME` antes de iniciar:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

Quando o log indicar que a aplicação iniciou, os endereços locais são:

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Definição OpenAPI | http://localhost:8080/v3/api-docs |
| Console H2 (desenvolvimento) | http://localhost:8080/h2-console |
| Painel do RabbitMQ | http://localhost:15672 (usuário `guest`, senha `guest`) |

No console H2, use JDBC URL `jdbc:h2:file:./data/refugio360;AUTO_SERVER=TRUE`, usuário `sa` e senha vazia. Os dados ficam na pasta `data/` e persistem entre reinicializações.

## Configuração

A aplicação inicia localmente sem Firebase ou chave Gemini. Defina as variáveis abaixo para integrar serviços externos e preparar outro ambiente:

| Variável | Uso | Padrão local |
|---|---|---|
| `FIREBASE_PROJECT_ID` | ID do projeto Firebase que emite os tokens aceitos | vazio (Firebase desabilitado) |
| `GEMINI_API_KEY` | Chave da API Gemini usada pelo Spring AI | sem chave (orientação alternativa) |
| `GEMINI_MODEL` | Modelo Gemini | `gemini-2.5-flash` |
| `CORS_ALLOWED_ORIGINS` | Origens do front, separadas por vírgula | `http://localhost:3000,http://localhost:5173,http://localhost:8081` |
| `DATABASE_URL` | URL JDBC do banco | H2 em arquivo `./data/refugio360` |
| `GESTOR_EMAIL` | E-mail do gestor inicial | `gestor@refugio360.local` |
| `GESTOR_PASSWORD` | Senha local do gestor inicial | `gestor12345` |
| `JWT_SECRET` | Chave dos JWTs locais; use segredo aleatório com pelo menos 32 bytes fora do desenvolvimento | valor de desenvolvimento |
| `DEMO_SEED_ENABLED` | Habilita o seed fictício quando o profile `dev` ou `demo` também está ativo | `false` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | Endereço do RabbitMQ | `localhost` / `5672` |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Credenciais do RabbitMQ | `guest` / `guest` |

Exemplo no PowerShell:

```powershell
$env:FIREBASE_PROJECT_ID = 'id-do-projeto-firebase'
$env:GEMINI_API_KEY = 'sua-chave-gemini'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:3000,http://localhost:5173'
.\mvnw.cmd spring-boot:run
```

O `FIREBASE_PROJECT_ID` precisa ser o mesmo configurado no app. O valor de CORS deve corresponder exatamente à origem usada pelo front.

## Seed de locais seguros para demonstração

O seed opcional disponibiliza seis locais fictícios para validar a listagem e o detalhe no Mobile quando um banco de desenvolvimento está vazio ou parcialmente preenchido. Ele não representa locais oficiais, disponibilidade, capacidade ou segurança real.

A execução exige duas autorizações simultâneas:

- profile Spring `dev` ou `demo`, sem o profile `prod` ativo;
- `refugio360.seed.demo-enabled=true`, normalmente pela variável `DEMO_SEED_ENABLED=true`.

Antes dos comandos com o profile `demo`, configure `JWT_SECRET`, `GESTOR_EMAIL` e `GESTOR_PASSWORD` no ambiente; `application-demo.properties` exige esses valores externos.

Linux ou macOS:

```bash
DEMO_SEED_ENABLED=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows PowerShell:

```powershell
$env:DEMO_SEED_ENABLED = 'true'
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

Para desabilitar, remova os profiles `dev`/`demo`, remova a variável ou defina `DEMO_SEED_ENABLED=false`. O profile `prod` bloqueia o componente mesmo se outro profile autorizado for informado, e `application-prod.properties` mantém a propriedade desabilitada.

No Render, **não** combine `demo` e `prod`: o perfil `demo` protegido substitui a configuração sensível herdada do arquivo base. A flag do seed permanece `false` por padrão; habilite-a explicitamente no painel do serviço.

O seed cadastra:

- `DEMO - Escola Municipal Abrigo Norte`;
- `DEMO - Centro Comunitário Esperança`;
- `DEMO - Ginásio de Apoio Central`;
- `DEMO - Unidade de Apoio Comunitário Sul`;
- `DEMO - Ponto de Acolhimento Jardim`;
- `DEMO - Centro de Atendimento Temporário`.

Cada item possui nome, tipo, endereço fictício, coordenadas válidas na área de demonstração de São Paulo e descrição explícita de conteúdo fictício. Como `LocalSeguro` não possui uma chave imutável de negócio, a idempotência usa o nome DEMO, ignorando maiúsculas e minúsculas: cada nome é consultado individualmente, apenas ausentes são inseridos, e registros encontrados nunca são atualizados. Se um nome do seed for alterado manualmente, a próxima execução poderá considerar o nome original ausente; não há alteração de schema ou constraint única para evitar esse caso operacional.

Para uma validação descartável, use H2 em memória em vez do banco local em arquivo:

```bash
DEMO_SEED_ENABLED=true DATABASE_URL='jdbc:h2:mem:refugio360-demo;DB_CLOSE_DELAY=-1' \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

## Autenticação

### Integração opcional com Firebase (não usada pelo Mobile atual)

1. O usuário cria a conta ou entra pelo Firebase Authentication.
2. O front obtém o ID token atualizado com `user.getIdToken()`.
3. Na primeira entrada, chama `POST /api/auth/firebase` com o token no header. A API cria o perfil local como `CIDADAO`, ou vincula um cadastro de gestor previamente configurado.
4. Nas próximas chamadas protegidas, envia o ID token Firebase no mesmo header.

Exemplo para um cliente que opte por Firebase (não é o fluxo do Mobile atual):

```ts
import { getAuth, signInWithEmailAndPassword } from "firebase/auth";

// Defina a URL HTTPS da API Java no cliente que estiver usando este fluxo opcional.
const API_URL = process.env.EXPO_PUBLIC_JAVA_API_URL;
const { user } = await signInWithEmailAndPassword(getAuth(), email, senha);
const idToken = await user.getIdToken();

const resposta = await fetch(`${API_URL}/api/auth/firebase`, {
  method: "POST",
  headers: { Authorization: `Bearer ${idToken}` },
});
if (!resposta.ok) throw new Error("Não foi possível vincular a conta à API");
const perfil = await resposta.json(); // { email, papel, firebaseUid }
```

Para chamar uma rota protegida depois do vínculo:

```ts
const token = await user.getIdToken();
const resposta = await fetch(`${API_URL}/api/ocorrencias?page=0&size=10`, {
  headers: { Authorization: `Bearer ${token}` },
});
const pagina = await resposta.json();
```

O papel não pode ser escolhido no app. Novos usuários Firebase viram `CIDADAO`. Para reconhecer um gestor pelo Firebase, configure `GESTOR_EMAIL` com o e-mail dele antes de iniciar a API; esse e-mail precisa estar verificado no Firebase. A senha inicial local não é utilizada no login pelo Firebase.

### Login local para o Mobile, Swagger e desenvolvimento

O Mobile usa estas rotas sem configurar Firebase. O cadastro público sempre cria um `CIDADAO`:

```http
POST /api/auth/register
Content-Type: application/json

{"email":"ana@example.com","senha":"senha-com-8-ou-mais"}
```

Faça login para receber um JWT local:

```http
POST /api/auth/login
Content-Type: application/json

{"email":"ana@example.com","senha":"senha-com-8-ou-mais"}
```

A resposta contém `accessToken`, `tokenType`, `expiresAt`, `email`, `papel` e `firebaseUid`. No Swagger, clique em **Authorize** e cole o valor de `accessToken` (sem escrever `Bearer`; o Swagger adiciona o prefixo). O token local expira em uma hora. No profile `demo`, o gestor inicial exige e-mail e senha configurados externamente.

## Perfis e acesso

| Operação | Acesso |
|---|---|
| Consultar locais seguros e pontos de risco | Público |
| Criar, editar e excluir locais seguros e pontos de risco | `GESTOR` |
| Criar ocorrências; consultar as próprias | `CIDADAO` ou `GESTOR` |
| Consultar todas as ocorrências, incluindo legadas sem autor | `GESTOR` |
| Editar, excluir ou atualizar status de ocorrência | `GESTOR` |
| Ver ou gerar orientação de uma ocorrência | Autor da ocorrência ou `GESTOR` |
| Consultar o próprio perfil (`/api/auth/me`) | Usuário autenticado |

## Endpoints

Todos os `GET` de coleção aceitam paginação Spring Data. Os parâmetros padrão são `page=0` e `size=10`; `page` começa em zero. Use `sort=campo,asc` ou `sort=campo,desc` para ordenar.

| Recurso | Método e rota | Acesso |
|---|---|---|
| Autenticação | `POST /api/auth/register` | Público; cria cidadão local |
| Autenticação | `POST /api/auth/login` | Público; login local |
| Autenticação | `POST /api/auth/firebase` | Token Firebase válido |
| Autenticação | `GET /api/auth/me` | Autenticado |
| Locais seguros | `GET /api/locais-seguros`, `GET /api/locais-seguros/{id}` | Público |
| Locais seguros | `POST /api/locais-seguros`, `PUT /api/locais-seguros/{id}`, `DELETE /api/locais-seguros/{id}` | `GESTOR` |
| Ocorrências | `GET /api/ocorrencias`, `GET /api/ocorrencias/{id}`, `POST /api/ocorrencias` | Autenticado; cidadão lê somente as próprias, gestor lê todas |
| Ocorrências | `GET /api/ocorrencias/minhas` | Autenticado; só as do próprio usuário |
| Ocorrências | `GET /api/ocorrencias/{id}/orientacoes` | Autor ou `GESTOR` |
| Ocorrências | `PUT /api/ocorrencias/{id}`, `PATCH /api/ocorrencias/{id}/status`, `DELETE /api/ocorrencias/{id}` | `GESTOR` |
| Pontos de risco | `GET /api/pontos-risco`, `GET /api/pontos-risco/{id}` | Público |
| Pontos de risco | `POST /api/pontos-risco`, `PUT /api/pontos-risco/{id}`, `DELETE /api/pontos-risco/{id}` | `GESTOR` |
| IA | `POST /api/ai/ocorrencias/{id}/orientacao?perfil=IDOSO` | Autor ou `GESTOR` |

### Exemplos de corpos

Criar local seguro (`POST /api/locais-seguros`, gestor):

```json
{
  "nome": "Abrigo Central",
  "tipo": "ABRIGO",
  "endereco": "Rua A, 100",
  "latitude": -23.55,
  "longitude": -46.63,
  "descricao": "Entrada acessível"
}
```

Registrar ocorrência (`POST /api/ocorrencias`, cidadão ou gestor):

```json
{
  "descricao": "Alagamento na avenida",
  "urgencia": "ALTA",
  "latitude": -23.55,
  "longitude": -46.63,
  "perfil": "IDOSO"
}
```

`perfil` é opcional (`GERAL`, `IDOSO`, `PCD` ou `FAMILIA`) e define para quem a orientação automática é escrita.

Atualizar status (`PATCH /api/ocorrencias/{id}/status`, gestor):

```json
{"status":"EM_ANALISE"}
```

Criar ponto de risco (`POST /api/pontos-risco`, gestor):

```json
{
  "titulo": "Área sujeita a alagamento",
  "nivelPerigo": "ALTO",
  "latitude": -23.55,
  "longitude": -46.63
}
```

O status segue um ciclo fixo: `PENDENTE` → `EM_ANALISE` → `RESOLVIDA` (também é possível resolver direto de `PENDENTE`, ou devolver `EM_ANALISE` para `PENDENTE`). Uma ocorrência `RESOLVIDA` não reabre; qualquer outra transição retorna `409`.

Urgências aceitas para ocorrências: `BAIXA`, `MEDIA`, `ALTA` e `CRITICA`. Níveis de perigo aceitos: `BAIXO`, `MODERADO`, `ALTO` e `CRITICO`. Latitude e longitude devem estar dentro dos limites geográficos válidos.

## Orientações com IA e RabbitMQ

O cidadão não precisa pedir a orientação: ela é gerada assim que o relato é registrado, sem travar a resposta do app.

```text
App ──POST /api/ocorrencias──▶ API grava a ocorrência ──(após o commit)──▶ RabbitMQ
                                                                            │  exchange refugio360.eventos
                                                                            │  fila refugio360.ocorrencias.orientacao
                                                                            ▼
                                     OrientacaoListener ──▶ Spring AI (Gemini) ──▶ grava a Orientacao
App ──GET /api/ocorrencias/{id}/orientacoes──▶ lê a orientação gerada
```

1. `POST /api/ocorrencias` grava o relato ligado ao autor e devolve `201` na hora.
2. Depois do commit, a API publica o evento `OcorrenciaRegistrada` no RabbitMQ.
3. O consumidor (`OrientacaoListener`) chama o Spring AI com o relato, a urgência, o perfil e os três locais seguros mais próximos, e grava o resultado na entidade `Orientacao`.
4. O app busca a orientação em `GET /api/ocorrencias/{id}/orientacoes`.

`POST /api/ai/ocorrencias/{id}/orientacao?perfil=IDOSO` gera uma nova orientação na hora, por exemplo para outro público. Somente o autor ou gestor pode consultar/gerar orientações; ocorrências legadas sem autor ficam restritas ao gestor. O texto muda conforme o perfil: frases curtas e passos numerados para `IDOSO`, trajetos acessíveis para `PCD` e cuidados com crianças para `FAMILIA`.

Com `GEMINI_API_KEY` configurada, a resposta pode ser gerada pelo Gemini. Sem chave ou se o provedor estiver indisponível, a API retorna uma orientação local e informa `geradoPorIA: false`. A resposta também inclui `locaisConsiderados` com os IDs dos locais usados como contexto. A IA não confirma horário, capacidade ou disponibilidade dos locais; valide essas informações antes do deslocamento.

O fallback local só acontece **quando a geração é acionada**: automaticamente pelo consumidor RabbitMQ após a criação de ocorrência, ou manualmente pelo endpoint POST acima. Sem broker, o POST de ocorrência continua salvando, mas não haverá orientação automática; o Mobile consulta apenas GET e não aciona o POST manual. Não há comprovação de broker/Gemini configurados no Render apenas pelos arquivos deste repositório.

## Demonstração protegida no serviço Java existente do Render

`application-demo.properties` herda do arquivo base H2 em arquivo (`./data/refugio360` no diretório `/app` da imagem), JPA `ddl-auto=update`, Gemini opcional e porta padrão; sobrescreve o console H2 para `false`, CORS para nenhuma origem web por padrão e exige JWT e credenciais do gestor de fora da imagem. O Dockerfile existente escuta em `0.0.0.0` e injeta `-Dserver.port=${PORT:-10000}`; não é necessário mudar o Dockerfile nem criar outro serviço. O profile `prod` não deve estar ativo junto com `demo`.

Variáveis do **mesmo serviço** Render, definidas em Environment (não inserir valores reais no Git):

| Variável | Obrigatória no MVP demo | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=demo` | Sim | Seleciona o profile protegido, sem `prod` |
| `DEMO_SEED_ENABLED=true` | Sim para mostrar os seis locais | Habilitação explícita do seed aditivo |
| `JWT_SECRET` | Sim, secreta | Chave estável de pelo menos 32 bytes UTF-8; manter igual nos reinícios |
| `GESTOR_EMAIL` | Sim | E-mail do gestor bootstrap; não usar o padrão local |
| `GESTOR_PASSWORD` | Sim, secreta | Senha forte do gestor bootstrap; não usar o padrão local |
| `DATABASE_URL` | Opcional | JDBC H2 em arquivo; se ausente, usa o caminho acima. Conferir o valor já configurado antes de alterar |
| `FIREBASE_PROJECT_ID` | Opcional | Só para aceitar tokens Firebase; login local não depende dela |
| `RABBITMQ_LISTENER_ENABLED=true` | Só se houver broker acessível | Default `false` no demo; sem consumidor não existe geração automática |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Com broker; usuário/senha secretos | Conexão AMQP; não usar `guest` em broker público |
| `GEMINI_API_KEY` | Opcional, secreta | Só gera texto por IA quando a orientação for acionada; sem chave, usa fallback local |
| `GEMINI_MODEL` | Opcional | Padrão `gemini-2.5-flash` |
| `CORS_ALLOWED_ORIGINS` | Opcional | Apenas para front web: origens HTTPS explícitas separadas por vírgula; app nativo não exige CORS |
| `PORT` | Fornecida pelo Render | O Dockerfile já a utiliza; não precisa fixar manualmente |

As credenciais JDBC H2 (`sa` e senha vazia) permanecem herdadas do banco local existente; o H2 é embutido e seu console fica desativado no demo. Não altere as credenciais JDBC de um arquivo H2 existente sem procedimento próprio de migração. Não há garantia de persistência do arquivo H2 no filesystem efêmero do Render gratuito: após reinício/redeploy podem desaparecer contas e ocorrências; o seed repõe somente os seis locais fictícios. O segredo JWT deve permanecer estável, mas não recupera contas apagadas.

### Execução local isolada (sem tocar `./data`)

Defina `JWT_SECRET`, `GESTOR_EMAIL` e `GESTOR_PASSWORD` como variáveis privadas de ambiente antes de executar; `JWT_SECRET` precisa ter pelo menos 32 bytes. Em seguida:

```bash
SPRING_PROFILES_ACTIVE=demo DEMO_SEED_ENABLED=true \
DATABASE_URL='jdbc:h2:mem:refugio360-demo;DB_CLOSE_DELAY=-1' \
./mvnw spring-boot:run
```

Para testar a imagem Docker após construí-la, injete as mesmas três variáveis por ambiente local (sem colocá-las na linha de comando ou em arquivo versionado):

```bash
docker build -t refugio360-java:demo .
docker run --rm -p 10000:10000 --env-file /caminho/privado/demo.env \
  -e SPRING_PROFILES_ACTIVE=demo -e DEMO_SEED_ENABLED=true \
  -e 'DATABASE_URL=jdbc:h2:mem:refugio360-demo;DB_CLOSE_DELAY=-1' refugio360-java:demo
```

O arquivo privado para `--env-file` deve conter `JWT_SECRET`, `GESTOR_EMAIL` e `GESTOR_PASSWORD`; não versioná-lo nem copiar credenciais reais para os comandos. Em execução local descartável, consulte `http://localhost:10000/api/locais-seguros?size=10`: `totalElements` deve ser `6` se não havia dados; os seis nomes começam por `DEMO -`. Repetir a inicialização no mesmo banco não duplica registros.

### Atualização manual do serviço existente

1. Revise e incorpore as alterações locais deste repositório; execute `mvn test`, `mvn package` e, se disponível, `docker build -t refugio360-java:demo .`. Envie a versão aprovada ao repositório Git usado pelo **serviço Java existente**.
2. No painel Render, abra o serviço Java já criado; em **Environment**, confira o `DATABASE_URL` atual e defina as variáveis da tabela. Use apenas `SPRING_PROFILES_ACTIVE=demo`; configure o segredo JWT e credenciais do gestor externamente. Deixe o listener desligado enquanto não houver RabbitMQ acessível.
3. Em **Settings**, mantenha o Dockerfile e branch do serviço existentes. Se desejar health check HTTP, use `/api/locais-seguros` (consulta pública ao banco); não há Actuator/rota `/health` nesta API.
4. Acione o deploy **desse mesmo serviço** pelo painel. Inspecione os logs para confirmar o profile `demo`, a porta atribuída, a inicialização do H2 e a linha `Seed DEMO de locais seguros concluído`. Sem broker, pode haver avisos ao publicar ocorrências; não confundir fallback Gemini com disponibilidade do RabbitMQ.
5. Na URL HTTPS **real** do serviço, confira `GET /api/locais-seguros?size=10`, registre um cidadão descartável em `/api/auth/register`, autentique em `/api/auth/login` e confirme `/api/auth/me`. Com dois cidadãos distintos, verifique acesso próprio `200`, alheio `403`, anônimo `401`, inexistente `404` e listagem restrita; gestor pode consultar todas. Use contas de demonstração apenas em banco descartável.
6. Só se houver broker provisionado, configure conexão e `RABBITMQ_LISTENER_ENABLED=true`, então valide na prática a criação e consulta de orientação. Com `GEMINI_API_KEY` ausente, o consumidor gera fallback local (`geradoPorIA=false`).

## Paginação e erros

Exemplo de listagem paginada:

```http
GET /api/locais-seguros?page=0&size=10&sort=nome,asc
```

As respostas de coleção seguem o formato de página do Spring Data, com conteúdo em `content` e informações como `totalElements`, `totalPages`, `number` e `size`.

Erros são retornados em JSON padronizado. Erro de validação inclui um mapa `fields` com os campos que precisam de correção:

```json
{
  "timestamp": "2026-11-11T20:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Revise os campos enviados",
  "path": "/api/ocorrencias",
  "fields": {"latitude": "deve ser maior ou igual a -90.0"}
}
```

`401` indica token ausente ou inválido; `403` indica perfil sem permissão; `404` indica recurso não encontrado; `409` indica conflito, como e-mail já cadastrado.

## Organização do código

```text
src/main/java/org/fiap/gs_final/
  controller/  endpoints REST e contratos HTTP
  service/     regras de negócio e autenticação
  repository/  persistência com Spring Data JPA
  model/       entidades persistidas no H2
  dto/         contratos de entrada e validações
  security/    Firebase, JWT local, perfis e CORS
  exception/   formato global de erros da API
  ai/          geração de orientações com Spring AI
  mensageria/  RabbitMQ: evento, publicação e consumidor
  config/      Swagger/OpenAPI e paginação
```

## Modelo de dados

| Entidade | Relacionamentos |
|---|---|
| `Usuario` | autor de várias `Ocorrencia` |
| `Ocorrencia` | `@ManyToOne` autor (`Usuario`); `@OneToMany` orientações (removidas junto) |
| `Orientacao` | `@ManyToOne` ocorrência; `@ManyToMany` locais seguros considerados |
| `LocalSeguro` | citado por várias `Orientacao` |
| `PontoRisco` | áreas de risco cadastradas pelo gestor |

## Equipe

- Gabriely Bonfim Silva — RM566242
- Henrique Rodrigues Vespasiano — RM562917
- Mirelly Sousa Alves — RM566299
- Ruan Luca Feliciano — RM562218
- Murilo Macedo Silva — RM566462

## Tecnologias

Java 21 · Spring Boot 3.4 · Spring Web · Spring Security · Spring Data JPA · H2 · Spring AI · Gemini · RabbitMQ (Spring AMQP) · Docker Compose · Firebase Authentication · Swagger/OpenAPI · Maven
