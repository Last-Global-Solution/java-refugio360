# Refúgio 360 — API

API REST do **Refúgio 360**, solução CitizenTech para apoiar comunidades durante ondas de calor, enchentes e falta de energia. O serviço cadastra locais seguros, recebe ocorrências, mapeia pontos de risco e gera orientações contextualizadas para cidadãos e gestores.

O projeto foi organizado no estilo do OlliPet: este README explica onde cada responsabilidade fica, como o app se autentica, quais rotas existem e como executar a API localmente.

## Como os dados e a autenticação funcionam

O front usa Firebase Authentication para cadastro e login. A API valida o ID token emitido pelo Firebase e mantém no H2 o perfil do usuário e os dados do domínio.

| Responsabilidade | Onde fica |
|---|---|
| Cadastro e login no aplicativo | Firebase Authentication |
| Validação do token e perfil `CIDADAO`/`GESTOR` | API Java com Spring Security |
| Locais seguros, ocorrências e pontos de risco | API Java + H2 |
| Orientações personalizadas | API Java + Spring AI/Gemini (com alternativa local) |

O front não precisa guardar senha na API nem fazer um segundo login. Depois do login Firebase, envia o ID token no header `Authorization: Bearer <token>`.

## Executar localmente

Requisitos: **JDK 21**. Na raiz do projeto, execute no PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

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

Exemplo no PowerShell:

```powershell
$env:FIREBASE_PROJECT_ID = 'id-do-projeto-firebase'
$env:GEMINI_API_KEY = 'sua-chave-gemini'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:3000,http://localhost:5173'
.\mvnw.cmd spring-boot:run
```

O `FIREBASE_PROJECT_ID` precisa ser o mesmo configurado no app. O valor de CORS deve corresponder exatamente à origem usada pelo front.

## Autenticação

### Integração com Firebase (fluxo do aplicativo)

1. O usuário cria a conta ou entra pelo Firebase Authentication.
2. O front obtém o ID token atualizado com `user.getIdToken()`.
3. Na primeira entrada, chama `POST /api/auth/firebase` com o token no header. A API cria o perfil local como `CIDADAO`, ou vincula um cadastro de gestor previamente configurado.
4. Nas próximas chamadas protegidas, envia o ID token Firebase no mesmo header.

Exemplo em JavaScript:

```js
import { getAuth, signInWithEmailAndPassword } from "firebase/auth";

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
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

```js
const token = await user.getIdToken();
const resposta = await fetch(`${API_URL}/api/ocorrencias?page=0&size=10`, {
  headers: { Authorization: `Bearer ${token}` },
});
const pagina = await resposta.json();
```

O papel não pode ser escolhido no app. Novos usuários Firebase viram `CIDADAO`. Para reconhecer um gestor pelo Firebase, configure `GESTOR_EMAIL` com o e-mail dele antes de iniciar a API; esse e-mail precisa estar verificado no Firebase. A senha inicial local não é utilizada no login pelo Firebase.

### Login local para Swagger e desenvolvimento

Para testar sem configurar Firebase, a API oferece cadastro e login locais. O cadastro público sempre cria um `CIDADAO`:

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

A resposta contém `accessToken`, `tokenType`, `expiresAt`, `email`, `papel` e `firebaseUid`. No Swagger, clique em **Authorize** e cole o valor de `accessToken` (sem escrever `Bearer`; o Swagger adiciona o prefixo). O token local expira em uma hora. O gestor de demonstração é criado ao iniciar, com os valores locais da tabela de configuração.

## Perfis e acesso

| Operação | Acesso |
|---|---|
| Consultar locais seguros e pontos de risco | Público |
| Criar, editar e excluir locais seguros e pontos de risco | `GESTOR` |
| Criar e consultar ocorrências | `CIDADAO` ou `GESTOR` |
| Editar, excluir ou atualizar status de ocorrência | `GESTOR` |
| Gerar orientação para uma ocorrência | `CIDADAO` ou `GESTOR` |
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
| Ocorrências | `GET /api/ocorrencias`, `GET /api/ocorrencias/{id}`, `POST /api/ocorrencias` | Autenticado |
| Ocorrências | `PUT /api/ocorrencias/{id}`, `PATCH /api/ocorrencias/{id}/status`, `DELETE /api/ocorrencias/{id}` | `GESTOR` |
| Pontos de risco | `GET /api/pontos-risco`, `GET /api/pontos-risco/{id}` | Público |
| Pontos de risco | `POST /api/pontos-risco`, `PUT /api/pontos-risco/{id}`, `DELETE /api/pontos-risco/{id}` | `GESTOR` |
| IA | `POST /api/ai/ocorrencias/{id}/orientacao?perfil=idoso` | `CIDADAO` ou `GESTOR` |

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
  "longitude": -46.63
}
```

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

Urgências aceitas para ocorrências: `BAIXA`, `MEDIA`, `ALTA` e `CRITICA`. Níveis de perigo aceitos: `BAIXO`, `MODERADO`, `ALTO` e `CRITICO`. Latitude e longitude devem estar dentro dos limites geográficos válidos.

## Orientações com IA

`POST /api/ai/ocorrencias/{id}/orientacao?perfil=idoso` usa o relato e a urgência da ocorrência, além de até três locais seguros cadastrados mais próximos. O parâmetro `perfil` é opcional e pode adaptar o texto ao público, por exemplo `idoso`, `pcd` ou `familia`.

Com `GEMINI_API_KEY` configurada, a resposta pode ser gerada pelo Gemini. Sem chave ou se o provedor estiver indisponível, a API retorna uma orientação local e informa `geradoPorIA: false`. A resposta também inclui `locaisConsiderados` com os IDs dos locais usados como contexto. A IA não confirma horário, capacidade ou disponibilidade dos locais; valide essas informações antes do deslocamento.

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
  config/      Swagger/OpenAPI e paginação
```

## Equipe

- Gabriely Bonfim Silva — RM566242
- Henrique Rodrigues Vespasiano — RM562917
- Mirelly Sousa Alves — RM566299
- Ruan Luca Feliciano — RM562218
- Murilo Macedo Silva — RM566462

## Tecnologias

Java 21 · Spring Boot 3.4 · Spring Web · Spring Security · Spring Data JPA · H2 · Spring AI · Gemini · Firebase Authentication · Swagger/OpenAPI · Maven
