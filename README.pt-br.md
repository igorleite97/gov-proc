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
- Monólito modular com 8 pacotes por bounded context, sem camadas de abstração desnecessárias

---

## Arquitetura

```
com.govproc
├── auth/           Autenticação JWT, papéis (ADMIN, MANAGER, ANALYST, VIEWER)
├── process/        Entidade central + máquina de 11 estados
├── analysis/       Análise de viabilidade operacional (1:1 por processo)
├── quotation/      Registros de custo por fornecedor — SEM markup, SEM margem
├── supplier/       Cadastro independente de fornecedores
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
  [activateContract()]
       │
  CONTRACT_ACTIVE
       │
   [close()]
       │
    CLOSED
```

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
| Testes | JUnit 5 + Mockito |
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

---

## Status dos Módulos

| Módulo | Status | Migrações | Testes |
|---|---|---|---|
| `shared/` | ✅ Completo | — | — |
| `auth/` | ✅ Completo | V1 | 6 |
| `process/` | ✅ Completo | V2 | — |
| `timeline/` | ✅ Completo | V3 | — |
| `analysis/` | ✅ Completo | V4 | 6 |
| `audit/` | ✅ Completo | V5 | — |
| `supplier/` | ✅ Completo | V6 | 2 |
| `quotation/` | ✅ Completo | V7 | 5 |
| `dispute/` | 🔲 Pendente | — | — |
| `postbid/` | 🔲 Pendente | — | — |
| `contract/` | 🔲 Pendente | — | — |

**Totais:** 62 classes · 19 testes · 7 migrações Flyway

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

---

## Roadmap

- [ ] **Módulo Dispute** — markup, margem, estratégia de lance (esses dados pertencem aqui, não na Cotação)
- [ ] **Módulo PostBid** — homologação, adjudicação, resultado final
- [ ] **Módulo Contract** — número do contrato, vigência, saldo, notas de empenho
- [ ] Snapshot de nome/documento do fornecedor na Cotação (para precisão histórica imutável)
- [ ] Índice parcial `WHERE selected = true` no PostgreSQL
- [ ] Testes de integração (fluxo completo, sem contexto Spring)
- [ ] Endpoint de promoção de papel (ANALYST → MANAGER → ADMIN)

---

## Licença

MIT License — Copyright (c) 2026 [Igor Leite de Andrade](https://github.com/igorleite)
