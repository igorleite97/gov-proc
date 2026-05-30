# GovProc

🌐 **Português** | [English](README.md)

> Plataforma backend para gestão de processos licitatórios públicos — da captura ao contrato — desenvolvida como projeto de portfólio demonstrando Domain-Driven Design, máquina de estados e rastreabilidade operacional com Java 21 e Spring Boot 3.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Máquina de Estados](#máquina-de-estados)
- [Tech Stack](#tech-stack)
- [Regras de Domínio Críticas](#regras-de-domínio-críticas)
- [Status dos Módulos](#status-dos-módulos)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Decisões Técnicas](#decisões-técnicas)
- [Roadmap](#roadmap)
- [Licença](#licença)

---

## Visão Geral

O GovProc modela o ciclo de vida completo de um processo licitatório público brasileiro. O sistema rastreia cada transição de estado desde a captura do edital até a ativação do contrato — passando por análise de viabilidade, cotação de fornecedores, disputa e pós-adjudicação — com trilha de auditoria completa e timeline operacional.

**O que este projeto demonstra:**

- Máquinas de estado encapsuladas nas próprias entidades (sem setter público de status)
- Separação rígida entre **custo** (Cotação) e **estratégia** (Disputa)
- Rastreabilidade dupla: `ProcessTimelineEvent` para eventos operacionais, `AuditLog` para alterações de campo
- `BigDecimal` com precisão `NUMERIC(19,4)` em todo o sistema — sem `double` para dinheiro
- Monólito modular com 11 pacotes por bounded context, sem camadas de abstração desnecessárias
- Cada fase complexa tem seu **próprio enum de status** (`AnalysisDecision`, `DisputeStatus`, `PostBidStatus`, `ContractStatus`) — o `ProcessStatus` central permanece enxuto, sem absorver cada sub-estado jurídico

---

## Arquitetura

```
com.govproc
├── auth/           Autenticação JWT, papéis (ADMIN, MANAGER, ANALYST, VIEWER)
├── process/        Entidade central + máquina de estados + read model do dashboard
├── analysis/       Análise de viabilidade operacional (1:1 por processo)
├── quotation/      Registros de custo por fornecedor — SEM markup, SEM margem
├── supplier/       Cadastro independente de fornecedores
├── dispute/        Estratégia comercial — margem, preço de venda, estratégia de lance
├── postbid/        Fase pós-disputa — homologação, adjudicação
├── contract/       Agregado Contract — ciclo de vida + empenhos, faturas, aditivos
├── timeline/       Log imutável de eventos operacionais
├── audit/          Log imutável de alterações por campo
└── shared/         BaseEntity, ApiResponse, exceções, handler global
```

**Decisões estruturais:**
- Pacotes por funcionalidade, não por camada técnica
- Sem split interface/implementação sem contrato real
- Sem `ApplicationEventPublisher` — orquestração de serviços explícita e legível
- `supplierId` armazenado como UUID em `Quotation` — sem `@ManyToOne`, sem risco de cascade

---

## Máquina de Estados

Toda transição de estado é um método de domínio em `ProcurementProcess` com validação interna. Não existe setter público para o campo `status`.

```
CAPTURED
   │
   └──[startAnalysis()]──► UNDER_ANALYSIS
                                │
              ┌─────────────────┴─────────────────┐
              │                                   │
   [approveAnalysis()]                  [rejectAnalysis(motivo)]
              │                                   │
       ANALYSIS_APPROVED               ANALYSIS_REJECTED
              │
   [startQuotation()]
              │
         IN_QUOTATION
              │
   [markAsQuoted()]  ← exige cotação selecionada
              │
           QUOTED
              │
   [startDispute()]
              │
          IN_DISPUTE
              │
       ┌──────┴──────┐
       │             │
  [markAsWinner()] [markAsLoser()]
       │             │
    WINNER         LOSER
       │
   [startPostBid()]
       │
     POST_BID   ← fase pós-disputa (PostBid: PENDING→HOMOLOGATED→ADJUDICATED→COMPLETED)
       │
   [activateContract()]  ← exige PostBid COMPLETED
       │
  CONTRACT_ACTIVE   ← fase contratual (Contract: ACTIVE→CLOSED/EXPIRED/TERMINATED)
       │
   [close()]
       │
    CLOSED
```

> **Repare na contenção deliberada:** homologação e adjudicação *não* são estados do processo — vivem dentro do bounded context `PostBid` (`PostBidStatus`). O processo apenas sabe que está em `POST_BID`. O mesmo vale para o ciclo do contrato (`ContractStatus`). Isso mantém o `ProcessStatus` em 11 valores em vez de deixá-lo crescer sem parar a cada etapa jurídica.

---

## Tech Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Segurança | Spring Security + JWT (jjwt 0.12.6) |
| Persistência | Spring Data JPA + Hibernate |
| Banco de Dados | PostgreSQL 16 |
| Migrações | Flyway |
| Validação | Jakarta Bean Validation |
| Documentação | SpringDoc OpenAPI 3 / Swagger UI |
| Testes | JUnit 5 + Mockito (unitários) · Testcontainers (integração) |
| Build | Maven |
| Container | Docker + Docker Compose |

---

## Regras de Domínio Críticas

### 1. Custo ≠ Estratégia

`Quotation` representa **apenas custo**. Contém:
- Fornecedor, fabricante, marca
- Custo unitário, frete, custo total
- Observações técnicas, prazo de entrega, quantidade

**Não contém** markup, margem, estratégia de preço ou agressividade de lance. Esses dados pertencem à **Disputa** — um bounded context completamente separado.

Esta é uma decisão de domínio deliberada e inegociável.

### 2. `selected` não é delete

Múltiplas cotações podem existir por processo. Quando uma é selecionada (`selected = true`), as demais são **desselecionadas, não deletadas**. O histórico completo de cotações é preservado para conformidade e rastreabilidade operacional.

### 3. `BigDecimal` sempre

Todos os valores monetários usam `BigDecimal` com precisão `NUMERIC(19,4)`. `double` e `float` são proibidos para dinheiro, custo, margem ou preço.

### 4. Sem Cascade ALL em Supplier

`Supplier` tem ciclo de vida próprio. `Quotation.supplierId` é armazenado como `UUID` puro — sem `@ManyToOne`, sem cascade. Um fornecedor não deve morrer porque uma cotação foi removida.

### 5. Empenho consome saldo — fatura não

No contrato público, são momentos distintos. O **empenho** reserva orçamento e **reduz** o `remainingBalance` — mesmo antes de qualquer pagamento. A **fatura** (liquidação) confirma a entrega; responde *"o fornecedor entregou?"*, não *"quanto ainda posso comprometer?"* — portanto **não** mexe no saldo. O **aditivo** altera a capacidade do contrato (`contractValue` + `remainingBalance` nos tipos de valor; `endDate` nos tipos de prazo). Esses invariantes vivem na raiz do agregado `Contract`.

---

## Status dos Módulos

| Módulo | Status | Migrações | Testes |
|---|---|---|---|
| `shared/` | ✅ Completo | — | — |
| `auth/` | ✅ Completo | V1 | 6 |
| `process/` | ✅ Completo | V2 | 5 |
| `timeline/` | ✅ Completo | V3 | — |
| `analysis/` | ✅ Completo | V4 | 6 |
| `audit/` | ✅ Completo | V5 | — |
| `supplier/` | ✅ Completo | V6 | 2 |
| `quotation/` | ✅ Completo | V7 | 5 |
| `dispute/` | ✅ Completo | V8 | 9 |
| `postbid/` | ✅ Completo | V9 | 8 |
| `contract/` | ✅ Completo | V10, V11 | 16 |

> Os testes em `process/` são do read model do dashboard (CQRS-lite); a máquina de estados central é exercitada pelos testes de workflow de cada módulo.

**Totais:** 118 classes · 60 testes (57 unitários + 3 integração) · 11 migrações Flyway

Os testes de integração (`GovProcIntegrationTest`) sobem o contexto Spring Boot completo contra um PostgreSQL real via **Testcontainers** — provando que as 11 migrations Flyway aplicam, que o mapeamento JPA valida (`ddl-auto=validate`), que a cadeia JWT funciona ponta a ponta e que constraints do banco (ex. `uq_process_number_uasg`) se manifestam corretamente.

---

## Como Executar

### Pré-requisitos

- Java 21
- Docker + Docker Compose
- Maven (ou use o wrapper incluso)

### 1. Subir o banco de dados

```bash
docker compose up -d
```

### 2. Executar a aplicação

```bash
./mvnw spring-boot:run
```

No Windows:
```bash
.\mvnw.cmd spring-boot:run
```

> Se `JAVA_HOME` não estiver configurado globalmente:
> ```powershell
> $env:JAVA_HOME = "C:\caminho\para\jdk-21"
> ```

### 3. Acessar a API

| URL | Descrição |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Documentação interativa da API |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI bruta |

### 4. Autenticar

```bash
# Registrar usuário
POST /auth/register
{
  "name": "Igor Andrade",
  "email": "igor@govproc.dev",
  "password": "password123"
}

# Login — retorna token JWT
POST /auth/login
{
  "email": "igor@govproc.dev",
  "password": "password123"
}
```

Use o token retornado como `Authorization: Bearer <token>` em todas as requisições seguintes.

---

## Endpoints da API

### Autenticação
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Registrar novo usuário (papel: ANALYST) |
| `POST` | `/auth/login` | Autenticar e receber JWT |

### Processos
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes` | Capturar novo processo licitatório |
| `GET` | `/processes` | Listar todos os processos |
| `GET` | `/processes/{id}` | Buscar processo por ID |
| `GET` | `/processes/{id}/timeline` | Histórico de eventos operacionais |
| `GET` | `/processes/{id}/audit` | Log de alterações por campo |

### Dashboard (somente leitura / CQRS-lite)
| Método | Caminho | Descrição |
|---|---|---|
| `GET` | `/dashboard/summary` | Contagens do pipeline + contratos ativos |
| `GET` | `/dashboard/financial` | Custo cotado, lucro esperado, valor contratado, saldo remanescente |
| `GET` | `/dashboard/performance` | Taxa de vitória, taxa de derrota, lucro esperado médio |

### Análise
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/analysis/start` | Iniciar análise → `UNDER_ANALYSIS` |
| `POST` | `/processes/{id}/analysis/approve` | Aprovar → `ANALYSIS_APPROVED` |
| `POST` | `/processes/{id}/analysis/reject` | Reprovar → `ANALYSIS_REJECTED` |
| `GET` | `/processes/{id}/analysis` | Consultar registro de análise |

### Cotação
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/quotation/start` | Iniciar fase de cotação → `IN_QUOTATION` |
| `POST` | `/processes/{id}/quotations` | Adicionar cotação de custo de fornecedor |
| `GET` | `/processes/{id}/quotations` | Listar todas as cotações (ordenadas por menor custo) |
| `PUT` | `/processes/{id}/quotations/{qid}/select` | Selecionar cotação vencedora |
| `POST` | `/processes/{id}/quotation/mark-quoted` | Marcar como cotado → `QUOTED` |

### Fornecedores
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/suppliers` | Cadastrar fornecedor |
| `GET` | `/suppliers` | Listar fornecedores ativos |
| `GET` | `/suppliers/{id}` | Buscar fornecedor por ID |
| `DELETE` | `/suppliers/{id}` | Desativar fornecedor (exclusão lógica) |

### Disputa
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/dispute/start` | Iniciar disputa → `IN_DISPUTE` (captura custo cotado) |
| `PUT` | `/processes/{id}/dispute` | Revisar estratégia comercial (auditoria campo a campo) |
| `POST` | `/processes/{id}/dispute/winner` | Marcar como vencedor → `WINNER` |
| `POST` | `/processes/{id}/dispute/loser` | Marcar como perdedor → `LOSER` |
| `GET` | `/processes/{id}/dispute` | Consultar registro da disputa |

### Pós-disputa (PostBid)
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/post-bid/start` | Iniciar fase pós-disputa → `POST_BID` |
| `POST` | `/processes/{id}/post-bid/homologate` | Homologar → `HOMOLOGATED` |
| `POST` | `/processes/{id}/post-bid/adjudicate` | Adjudicar → `ADJUDICATED` |
| `POST` | `/processes/{id}/post-bid/complete` | Concluir → `COMPLETED` |
| `GET` | `/processes/{id}/post-bid` | Consultar registro pós-disputa |

### Contrato
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/contract/activate` | Ativar contrato → `CONTRACT_ACTIVE` (exige PostBid COMPLETED) |
| `POST` | `/processes/{id}/contract/close` | Encerrar contrato e processo → `CLOSED` |
| `POST` | `/processes/{id}/contract/terminate` | Rescindir → contrato `TERMINATED`, processo `CLOSED` |
| `POST` | `/processes/{id}/contract/expire` | Vencer → contrato `EXPIRED`, processo `CLOSED` |
| `GET` | `/processes/{id}/contract` | Consultar registro do contrato |

### Execução do Contrato (membros do agregado)
| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/processes/{id}/contract/commitments` | Registrar empenho — **reduz o saldo** |
| `GET` | `/processes/{id}/contract/commitments` | Listar empenhos |
| `POST` | `/processes/{id}/contract/invoices` | Registrar fatura — **não** mexe no saldo |
| `GET` | `/processes/{id}/contract/invoices` | Listar faturas |
| `POST` | `/processes/{id}/contract/addenda` | Aplicar aditivo — alteração de valor ou prazo |
| `GET` | `/processes/{id}/contract/addenda` | Listar aditivos |

---

## Decisões Técnicas

### Sem split interface/implementação
`ProcessService`, não `ProcessService` + `ProcessServiceImpl`. Classes concretas. Mockito mocka classes concretas sem necessidade de interface.

### Sem ApplicationEventPublisher
Chamadas de timeline e auditoria são explícitas em cada método de serviço. O trade-off é verbosidade em troca de fluxo de controle 100% visível — sem "magia" de wiring.

### AuditLog não herda BaseEntity
`AuditLog` é um registro de conformidade imutável. Tem seu próprio campo `performedAt: Instant` definido no construtor. Usar `@LastModifiedDate` em um log de auditoria seria semanticamente incorreto.

### `totalCost` calculado e persistido
`totalCost = unitCost × quantity + shippingCost`, normalizado para escala 4 e salvo no banco. Se a fórmula mudar no futuro, os registros históricos permanecem precisos.

### Seleção atômica de cotação
`selectQuotation()` executa `clearSelectedByProcess(processId)` via `@Modifying @Query` antes de chamar `quotation.select()`. Isso garante exatamente um `selected = true` por processo sem janela de inconsistência.

### Sub-status ficam fora do `ProcessStatus`
Homologação, adjudicação e o ciclo do contrato *não* são estados do processo. Estão encapsulados em enums próprios (`PostBidStatus`, `ContractStatus`) dentro de seus bounded contexts. A máquina central só conhece `POST_BID` e `CONTRACT_ACTIVE`. É o mesmo raciocínio que mantém o `DisputeStatus` (OPEN/CONCLUDED) fora do processo — e evita que o `ProcessStatus` cresça sem parar.

### O guard de ativação do contrato vive no service
O método de domínio `activateContract()` só valida `POST_BID → CONTRACT_ACTIVE`. A regra *"somente quando a fase pós-disputa estiver COMPLETED"* vive no `ContractService`, porque o processo não conhece — e não deve conhecer — o `PostBid`. Invariantes entre contextos pertencem à camada de aplicação, não à entidade.

---

## Roadmap

- [x] **Módulo Dispute** — margem, preço de venda, estratégia de lance (esses dados pertencem aqui, não na Cotação)
- [x] **Módulo PostBid** — homologação, adjudicação, conclusão
- [x] **Módulo Contract** — número do contrato, vigência, valor, saldo, ciclo próprio
- [x] **Aprofundamento do agregado Contract** — empenhos (consumo de saldo), faturas, aditivos (valor/prazo)
- [x] **Dashboard / KPIs** — read model (CQRS-lite) com agregações operacionais, financeiras e de performance
- [x] **Testes de integração com Testcontainers** — PostgreSQL real, Flyway, cadeia JWT, constraints do banco
- [ ] Pipeline de CI (GitHub Actions — build + testes)
- [ ] Pagamentos e medições (deliberadamente fora de escopo — evita transformar o GovProc em ERP financeiro)
- [ ] Snapshot de nome/documento do fornecedor na Cotação (para precisão histórica imutável)
- [ ] Índice parcial `WHERE selected = true` no PostgreSQL
- [ ] Endpoint de promoção de papel (ANALYST → MANAGER → ADMIN)

---

## Licença

MIT License — Copyright (c) 2026 [Igor Leite de Andrade](https://github.com/igorleite)
