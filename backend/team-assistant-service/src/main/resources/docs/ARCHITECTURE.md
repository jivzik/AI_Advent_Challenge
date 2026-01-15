# B2B WebShop - System Architecture

> Last Updated: 2026-01-15  
> Version: 2.0  
> Author: Architecture Team

## Table of Contents
1. [Overview](#overview)
2. [System Components](#system-components)
3. [Microservices Architecture](#microservices-architecture)
4. [Data Flow](#data-flow)
5. [Authentication & Authorization](#authentication--authorization)
6. [Deployment Architecture](#deployment-architecture)
7. [Technology Stack](#technology-stack)

---

## Overview

B2B WebShop is a wholesale e-commerce platform designed for business-to-business transactions. The system handles:
- Product catalog management (50,000+ SKUs)
- Order processing (10,000+ orders/day)
- Multi-tier pricing and volume discounts
- Invoice generation and EDO integration
- Integration with external systems (1C, SAP)

### Key Requirements
- **Availability:** 99.9% uptime
- **Performance:** < 500ms response time for 95% of requests
- **Scalability:** Support 10,000 concurrent users
- **Security:** PCI DSS compliance for payment processing

---

## System Components

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        Load Balancer                          │
│                    (Nginx + SSL Termination)                  │
└────────────────────────┬─────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Frontend   │  │   Frontend   │  │   Frontend   │
│  (Vue 3 SPA) │  │  (Vue 3 SPA) │  │  (Vue 3 SPA) │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └────────────────┬┼────────────────┘
                        ││
                        ▼▼
               ┌─────────────────┐
               │   API Gateway   │
               │  (Spring Cloud) │
               └────────┬────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Catalog    │ │    Order     │ │   Payment    │
│   Service    │ │   Service    │ │   Service    │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
                        ▼
               ┌─────────────────┐
               │   PostgreSQL    │
               │   (Primary DB)  │
               └─────────────────┘
```

---

## Microservices Architecture

### 1. **API Gateway Service**
- **Port:** 8080
- **Technology:** Spring Cloud Gateway
- **Responsibilities:**
  - Request routing
  - Authentication/Authorization
  - Rate limiting
  - Request/Response transformation
  - API versioning

### 2. **Auth Service**
- **Port:** 8081
- **Technology:** Spring Boot 3 + Spring Security
- **Database:** PostgreSQL (users, roles, permissions)
- **Responsibilities:**
  - User authentication (JWT tokens)
  - Role-based access control (RBAC)
  - OAuth2 integration (Google, Microsoft)
  - Session management
  - Password policies

**Authentication Flow:**
```
1. User submits credentials → API Gateway
2. Gateway forwards → Auth Service
3. Auth Service validates credentials
4. If valid: Generate JWT token (1h expiry)
5. Return token + refresh token (30 days)
6. Client includes JWT in all subsequent requests
7. Gateway validates JWT signature
8. If valid → route to service
9. If expired → 401 Unauthorized
```

### 3. **Catalog Service**
- **Port:** 8082
- **Technology:** Spring Boot 3 + JPA
- **Database:** PostgreSQL (products, categories, prices)
- **Cache:** Redis (product data, 15 min TTL)
- **Responsibilities:**
  - Product catalog management
  - Category hierarchy
  - Price management (base price + customer tiers)
  - Inventory tracking
  - Product search (Elasticsearch integration)

**Key Tables:**
- `products` - Product master data
- `categories` - Category hierarchy
- `prices` - Tiered pricing (bronze/silver/gold/platinum)
- `inventory` - Stock levels by warehouse

### 4. **Order Service**
- **Port:** 8083
- **Technology:** Spring Boot 3 + JPA
- **Database:** PostgreSQL (orders, order_items)
- **Message Queue:** RabbitMQ (order events)
- **Responsibilities:**
  - Order creation and processing
  - Order status tracking
  - Delivery management
  - Invoice generation
  - EDO integration (electronic document flow)

**Order Status Flow:**
```
NEW → CONFIRMED → PAID → PACKED → SHIPPED → DELIVERED
                     ↓
                  CANCELLED
```

**Order Processing:**
1. Client creates order → POST /api/orders
2. Order Service validates:
   - Product availability
   - Credit limit
   - Minimum order amount
3. If valid: Create order (status: NEW)
4. Publish event → order.created
5. Payment Service listens → generates invoice
6. Customer pays → status: PAID
7. Warehouse packs → status: PACKED
8. Logistics ships → status: SHIPPED
9. Customer receives → status: DELIVERED

### 5. **Payment Service**
- **Port:** 8084
- **Technology:** Spring Boot 3
- **Database:** PostgreSQL (invoices, payments)
- **External:** Stripe API, PayPal API, Bank APIs
- **Responsibilities:**
  - Invoice generation
  - Payment processing
  - Payment gateway integration
  - Payment reconciliation
  - EDO invoice sending

**Payment Flow:**
```
1. Order confirmed → Invoice created
2. Invoice sent to customer email
3. Customer pays via:
   - Bank transfer (manual reconciliation)
   - Credit card (Stripe/PayPal)
   - Corporate account (credit limit)
4. Payment received → Payment record created
5. Order status updated → PAID
6. Notification sent to warehouse
```

### 6. **User Service**
- **Port:** 8085
- **Technology:** Spring Boot 3
- **Database:** PostgreSQL (company_profiles, contacts)
- **Responsibilities:**
  - Company profile management
  - Contact management
  - Credit limit tracking
  - Loyalty tier management
  - Customer preferences

**Loyalty Tiers:**
- **Bronze:** Default (0% discount)
- **Silver:** > 500k annual revenue (5% discount)
- **Gold:** > 2M annual revenue (10% discount)
- **Platinum:** > 5M annual revenue (15% discount + priority support)

### 7. **Notification Service**
- **Port:** 8086
- **Technology:** Spring Boot 3
- **Message Queue:** RabbitMQ
- **External:** SendGrid (email), Twilio (SMS)
- **Responsibilities:**
  - Email notifications
  - SMS notifications
  - Push notifications
  - Notification templates
  - Delivery tracking

**Notification Types:**
- Order confirmation
- Payment received
- Shipment tracking
- Invoice ready
- Price changes
- Promotional campaigns

### 8. **Support Service** (NEW - Day 22)
- **Port:** 8087
- **Technology:** Spring Boot 3 + RAG + MCP
- **Database:** PostgreSQL (support_tickets, ticket_messages)
- **AI:** OpenRouter (Claude 3.5 Sonnet)
- **Responsibilities:**
  - Customer support tickets
  - AI-powered FAQ responses
  - Knowledge base integration
  - Escalation to human agents

### 9. **Team Service** (NEW - Day 23)
- **Port:** 8088
- **Technology:** Spring Boot 3 + RAG + MCP
- **Database:** PostgreSQL (team_members, query_logs)
- **AI:** OpenRouter (Claude 3.5 Sonnet)
- **MCP:** Google Tasks integration
- **Responsibilities:**
  - Task management for dev team
  - Project knowledge search
  - Priority analysis
  - Development recommendations

---

## Data Flow

### Order Creation Flow

```
1. Frontend → POST /api/orders
   {
     "items": [
       {"productId": "P123", "quantity": 100}
     ],
     "deliveryAddress": {...}
   }

2. API Gateway → Validates JWT → Routes to Order Service

3. Order Service:
   a. Validate customer credit limit (User Service)
   b. Check product availability (Catalog Service)
   c. Calculate total price with discounts (Catalog Service)
   d. Create order record (status: NEW)
   e. Reserve inventory (Catalog Service)
   f. Publish event → order.created

4. Payment Service (listens to order.created):
   a. Generate invoice
   b. Send invoice email (Notification Service)

5. Response to Frontend:
   {
     "orderId": "ORD-2026-1234",
     "status": "NEW",
     "totalAmount": 15000.00,
     "invoiceUrl": "/invoices/INV-2026-5678.pdf"
   }
```

---

## Authentication & Authorization

### JWT Token Structure

```json
{
  "sub": "user@company.com",
  "name": "John Doe",
  "role": "CUSTOMER",
  "tier": "GOLD",
  "companyId": "COMP-123",
  "permissions": ["order:create", "order:view", "invoice:download"],
  "iat": 1704067200,
  "exp": 1704070800
}
```

### Authorization Levels

**Customer Roles:**
- `CUSTOMER_VIEWER` - View catalog, view own orders
- `CUSTOMER_BUYER` - + Create orders
- `CUSTOMER_ADMIN` - + Manage company users, view all company orders

**Internal Roles:**
- `SUPPORT_AGENT` - View tickets, respond to customers
- `WAREHOUSE_STAFF` - Pack orders, update status
- `FINANCE` - View invoices, process payments
- `ADMIN` - Full access

### API Endpoints Security

```java
// Public endpoints (no auth)
GET /api/catalog/products
GET /api/catalog/categories

// Authenticated endpoints
GET /api/orders (requires: order:view)
POST /api/orders (requires: order:create)
GET /api/invoices/{id} (requires: invoice:view, owns invoice)

// Admin endpoints
POST /api/admin/products (requires: ADMIN role)
DELETE /api/admin/users/{id} (requires: ADMIN role)
```

---

## Deployment Architecture

### Production Environment

**Infrastructure:**
- **Cloud Provider:** AWS
- **Region:** eu-central-1 (Frankfurt)
- **Availability:** Multi-AZ deployment

**Components:**
- **Load Balancer:** AWS ALB (Application Load Balancer)
- **Web Servers:** EC2 instances (t3.large, Auto Scaling)
- **Application Servers:** ECS Fargate containers
- **Database:** RDS PostgreSQL 15 (Multi-AZ, r6g.xlarge)
- **Cache:** ElastiCache Redis (cache.r6g.large)
- **Search:** OpenSearch (r6g.large)
- **Message Queue:** Amazon MQ (RabbitMQ)
- **Storage:** S3 (invoices, product images)

**Scaling:**
- Frontend: 2-10 instances (CPU-based auto-scaling)
- Backend services: 2-6 instances per service
- Database: Read replicas (2) for reporting queries

### Development Environment

- **Local:** Docker Compose
- **Staging:** Kubernetes (EKS)
- **CI/CD:** GitHub Actions → AWS ECR → ECS

---

## Technology Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.0
- **ORM:** Spring Data JPA + Hibernate
- **Database:** PostgreSQL 15
- **Cache:** Redis 7
- **Search:** Elasticsearch 8
- **Message Queue:** RabbitMQ 3.12
- **API:** REST + OpenAPI 3.0
- **Security:** Spring Security + JWT

### Frontend
- **Framework:** Vue 3 (Composition API)
- **Language:** TypeScript
- **Build:** Vite
- **UI Library:** Element Plus
- **State:** Pinia
- **HTTP Client:** Axios

### DevOps
- **Containerization:** Docker
- **Orchestration:** Kubernetes (EKS)
- **CI/CD:** GitHub Actions
- **Monitoring:** Prometheus + Grafana
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing:** Jaeger

### AI Integration (NEW)
- **LLM Provider:** OpenRouter
- **Models:** Claude 3.5 Sonnet, GPT-4
- **RAG:** Custom RAG service with pgvector
- **MCP:** Model Context Protocol servers
- **Vector DB:** PostgreSQL with pgvector extension

---

## Key Design Decisions

### 1. Microservices vs Monolith
**Decision:** Microservices architecture  
**Rationale:**
- Independent scaling of services
- Technology diversity (different services can use different stacks)
- Team autonomy (different teams own different services)
- Fault isolation (one service failure doesn't bring down entire system)

### 2. Synchronous vs Asynchronous Communication
**Decision:** Hybrid approach  
**Synchronous (REST):** Read operations, order creation  
**Asynchronous (Events):** Order processing, notifications, inventory updates  
**Rationale:** Balance between consistency and performance

### 3. Database per Service
**Decision:** Shared PostgreSQL with separate schemas  
**Rationale:**
- Easier transactions across services (same DB)
- Simplified deployment and management
- Cost-effective for current scale
- Plan to migrate to separate DBs as scale increases

### 4. Caching Strategy
**Decision:** Multi-layer caching  
**Layers:**
- Browser cache (static assets)
- CDN cache (product images)
- Redis cache (product data, session data)
- Application cache (reference data)

---

## Security Considerations

### Data Protection
- All passwords hashed with BCrypt (cost: 12)
- Sensitive data encrypted at rest (AES-256)
- TLS 1.3 for all communications
- Database encryption enabled

### PCI DSS Compliance
- Payment card data never stored
- Payment gateway integration (Stripe, PayPal)
- PCI SAQ-A compliance
- Annual security audits

### Access Control
- Role-Based Access Control (RBAC)
- Principle of least privilege
- Multi-factor authentication for admin accounts
- Session timeout: 1 hour

---

## Performance Optimization

### Database Optimization
- **Indexes:** All foreign keys, frequently queried columns
- **Partitioning:** Orders table partitioned by date
- **Connection Pooling:** HikariCP (max 20 connections per service)
- **Query Optimization:** N+1 query prevention, eager loading

### Caching Strategy
- **Product data:** 15 min TTL
- **Categories:** 1 hour TTL
- **Prices:** 5 min TTL (frequent updates)
- **Session data:** 1 hour TTL

### API Optimization
- **Pagination:** Default 20 items, max 100
- **Compression:** Gzip compression enabled
- **API Versioning:** URL-based (/api/v1/, /api/v2/)
- **Rate Limiting:** 1000 requests/hour per user

---

## Monitoring & Observability

### Metrics (Prometheus)
- Request rate, error rate, duration (RED metrics)
- Database connection pool usage
- Cache hit ratio
- JVM metrics (heap, GC)

### Logging (ELK)
- Application logs (INFO level in prod)
- Access logs
- Error logs with stack traces
- Audit logs (user actions)

### Tracing (Jaeger)
- End-to-end request tracing
- Service dependency mapping
- Performance bottleneck identification

---

## Disaster Recovery

### Backup Strategy
- **Database:** Automated daily backups (7 day retention)
- **Point-in-time recovery:** 5 minute granularity
- **Cross-region backup:** Weekly backups to us-east-1

### Recovery Time Objectives (RTO)
- **Critical services:** < 1 hour
- **Non-critical services:** < 4 hours

### Recovery Point Objectives (RPO)
- **Transactional data:** < 5 minutes
- **Non-critical data:** < 1 hour

---

## Future Architecture Plans

### Short-term (Q1 2026)
- [ ] Implement event sourcing for Order Service
- [ ] Add GraphQL API alongside REST
- [ ] Migrate to Kubernetes (EKS)
- [ ] Implement API rate limiting per customer tier

### Long-term (2026)
- [ ] Separate databases per service
- [ ] Implement CQRS for reporting
- [ ] Add real-time analytics with Apache Kafka
- [ ] Machine learning for demand forecasting

---

## Contact

**Architecture Team:**
- Lead Architect: architect@webshop.example.com
- Questions: #architecture on Slack

**Last Review:** 2026-01-15
