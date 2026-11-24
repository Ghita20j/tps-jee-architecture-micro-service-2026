# Projet – Architectures Micro-services (Spring Cloud + Spring AI + MCP)

Application bancaire **EBank** basée sur une architecture micro-services, avec un **Agent IA** (Spring AI) qui
accède aux fonctionnalités des micro-services via le protocole **MCP** et qui est accessible depuis
un client **REST**, **Telegram** et **Discord**.

## 1. Architecture

```
                 ┌──────────────────┐        ┌──────────────────┐
                 │ discovery-service│◄──────►│  config-service  │
                 │  Eureka  :8761   │        │  Config  :9999   │
                 └────────▲─────────┘        └──────────────────┘
                          │ enregistrement
   Postman / Angular      │
        │          ┌──────┴───────┐   lb://   ┌─────────────────────┐
        └─────────►│gateway :8888 ├──────────►│ customer-service    │ :8081  (REST + MCP /mcp)
                   └──────┬───────┘           └─────────▲───────────┘
                          │ lb://                       │ OpenFeign + Resilience4j
                          ▼                             │
                   ┌─────────────────────┐              │
                   │ account-service     ├──────────────┘
                   │ (EBank) :8082       │ (REST + MCP /mcp)
                   └─────────▲───────────┘
                             │ MCP (Streamable HTTP)
                   ┌─────────┴───────────┐     ┌──────────────┐
  REST / Telegram ►│ ebank-ai-agent :8070├────►│  OpenAI LLM  │
  / Discord        │ Spring AI + mémoire │     └──────────────┘
                   └─────────────────────┘
```

| Module | Port | Rôle |
|---|---|---|
| `discovery-service` | 8761 | Annuaire Eureka |
| `config-service` | 9999 | Configuration centralisée (mode `native`, dossier `config-repo`) |
| `gateway-service` | 8888 | Spring Cloud Gateway (routage statique + dynamique) |
| `customer-service` | 8081 | CRUD clients + Swagger + serveur MCP |
| `account-service` (EBank-Service) | 8082 | Comptes bancaires, OpenFeign → customer-service, Circuit Breaker Resilience4j, Swagger, serveur MCP |
| `ebank-ai-agent` | 8070 | Agent Spring AI (OpenAI) + client MCP + mémoire + REST/Telegram/Discord |

Stack : Java 21, Spring Boot 3.5.7, Spring Cloud 2025.0.0, Spring AI 1.1.0, H2, springdoc-openapi.

## 2. Prérequis

- JDK 21, Maven 3.9+
- Une clé OpenAI dans une **variable d'environnement** (ne jamais la mettre dans le code ni sur GitHub) :

```bash
# Linux / macOS
export OPENAI_API_KEY=sk-...
# Windows PowerShell
$env:OPENAI_API_KEY="sk-..."
```

## 3. Lancement (dans cet ordre)

```bash
mvn clean install -DskipTests
cd discovery-service && mvn spring-boot:run     # 1
cd config-service    && mvn spring-boot:run     # 2
cd customer-service  && mvn spring-boot:run     # 3
cd account-service   && mvn spring-boot:run     # 4
cd gateway-service   && mvn spring-boot:run     # 5
cd ebank-ai-agent    && mvn spring-boot:run     # 6 (après 3 et 4, car il se connecte aux serveurs MCP au démarrage)
```

Avec IntelliJ : ouvrir le dossier racine et lancer les classes `*Application` dans le même ordre.

## 4. Partie 1 – Tests des micro-services

### Eureka
http://localhost:8761 → CUSTOMER-SERVICE, ACCOUNT-SERVICE, GATEWAY-SERVICE, CONFIG-SERVICE doivent apparaître.

### Config service
- http://localhost:9999/customer-service/default
- http://localhost:9999/account-service/default

### Swagger-UI
- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html

### Postman (importer `postman/EBank.postman_collection.json`)

| Requête | URL |
|---|---|
| Clients (direct) | `GET http://localhost:8081/api/customers` |
| Client par id | `GET http://localhost:8081/api/customers/1` |
| Clients via gateway (statique) | `GET http://localhost:8888/api/customers` |
| Comptes via gateway (statique) | `GET http://localhost:8888/api/accounts` |
| Routage dynamique | `GET http://localhost:8888/account-service/api/accounts` |
| Comptes d'un client | `GET http://localhost:8888/api/accounts/customer/1` |
| Créer un compte | `POST http://localhost:8888/api/accounts` body `{"customerId":1,"initialBalance":5000,"currency":"MAD","type":"SAVING_ACCOUNT"}` |
| Versement | `POST http://localhost:8888/api/accounts/{id}/deposit?amount=1000` |
| Retrait | `POST http://localhost:8888/api/accounts/{id}/withdraw?amount=500` |
| Virement | `POST http://localhost:8888/api/accounts/transfer` body `{"fromAccountId":"..","toAccountId":"..","amount":100}` |

### Test du Circuit Breaker (Resilience4j)
1. `GET http://localhost:8082/api/accounts` → chaque compte contient son `customer`.
2. **Arrêter** `customer-service`.
3. Rappeler `GET http://localhost:8082/api/accounts` → le service répond toujours, avec
   `"firstName":"Not Available"` (fallback `CustomerRestClientFallback`).
4. Voir l'état du circuit : http://localhost:8082/actuator/circuitbreakers (`CLOSED` → `OPEN` → `HALF_OPEN`).
5. Redémarrer `customer-service` → après 10 s le circuit se referme.

## 5. Partie 2 – MCP + Agent IA

### Serveurs MCP
Chaque micro-service expose ses fonctionnalités via des méthodes `@Tool` (classes `CustomerMcpTools`,
`AccountMcpTools`) publiées par `spring-ai-starter-mcp-server-webmvc` sur l'endpoint **`/mcp`** (Streamable HTTP).

| Service | Tools |
|---|---|
| customer-service | `getAllCustomers`, `getCustomerById`, `searchCustomers`, `createCustomer` |
| account-service | `getAllAccounts`, `getAccountById`, `getAccountsByCustomerId`, `createAccount`, `deposit`, `withdraw`, `transfer` |

### Tester les micro-services via un client MCP (MCP Inspector)
```bash
npx @modelcontextprotocol/inspector
```
Transport **Streamable HTTP**, URL `http://localhost:8081/mcp` (puis `http://localhost:8082/mcp`) →
*Connect* → *List Tools* → exécuter `getAllCustomers`, `getAccountsByCustomerId` (customerId = 1)...

### Agent IA (client MCP)
`EbankAiAgent` utilise `ChatClient` + les tools MCP + `MessageChatMemoryAdvisor`.
Comme demandé pour Spring Boot 4, la mémoire utilise `ChatMemory.CONVERSATION_ID`
(une conversation par utilisateur : `telegram-<chatId>`, `discord-<channelId>`, ou celle passée en REST).

Tools découverts : http://localhost:8070/tools

### Tester le chatbot avec un client REST
```
GET  http://localhost:8070/chat?query=Donne moi la liste des clients
GET  http://localhost:8070/chat?query=Quels sont les comptes de Imane ?
GET  http://localhost:8070/chat?query=Verse 1000 MAD sur son compte courant
POST http://localhost:8070/api/chat   {"conversationId":"user1","message":"Quel est le solde total des comptes du client 1 ?"}
```
La 3e question teste la mémoire (« son » = Imane).

### Tester le chatbot avec Telegram
1. Sur Telegram, ouvrir **@BotFather** → `/newbot` → choisir un nom et un username (finissant par `bot`).
2. Copier le token, puis :
```bash
export TELEGRAM_ENABLED=true
export TELEGRAM_BOT_TOKEN=123456:ABC...
export TELEGRAM_BOT_USERNAME=MonEBankBot
```
3. Relancer `ebank-ai-agent`, ouvrir le bot, envoyer `/start` puis « liste des clients ».

### Tester le chatbot avec Discord
1. https://discord.com/developers/applications → *New Application* → onglet **Bot** → *Reset Token* (copier).
2. Dans **Bot**, activer **MESSAGE CONTENT INTENT**.
3. *OAuth2 → URL Generator* : scope `bot`, permissions `Send Messages`, `Read Message History`, `View Channels`
   → ouvrir l'URL générée pour inviter le bot sur votre serveur.
4. Variables :
```bash
export DISCORD_ENABLED=true
export DISCORD_BOT_TOKEN=MTEx...
```
5. Relancer l'agent. Dans un salon : `@EBankBot donne moi les comptes du client 2` (ou en message privé, sans mention).

## 6. Migration vers Spring Boot 4
Le code de l'agent suit déjà la syntaxe de l'énoncé (`ChatMemory.CONVERSATION_ID`).
Pour passer en Boot 4 : mettre `spring-boot-starter-parent` en 4.0.x, `spring-cloud.version` en 2025.1.x,
et `spring-ai.version` en 2.0.x dans le `pom.xml` racine.

## 7. Structure
```
ebank-microservices/
├── discovery-service/
├── config-service/          (src/main/resources/config-repo/*.properties)
├── gateway-service/
├── customer-service/        (entities, repositories, web, mcp)
├── account-service/         (entities, model, clients[Feign+fallback], services, web, mcp)
├── ebank-ai-agent/          (agents, web, telegram, discord)
└── postman/EBank.postman_collection.json
```
