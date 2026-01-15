# B2B WebShop Platform

> Wholesale e-commerce platform for business-to-business transactions  
> Version: 2.0  
> Last Updated: 2026-01-15

## 🎯 Overview

B2B WebShop is a modern wholesale platform designed for business clients. The system handles catalog management, order processing, multi-tier pricing, invoice generation, and integrations with external systems.

**Key Features:**
- 📦 Product catalog (50,000+ SKUs)
- 🛒 Order management (10,000+ orders/day)
- 💰 Multi-tier pricing and volume discounts
- 📄 Automated invoice generation
- 🔐 Role-based access control
- 🤖 AI-powered support assistant
- 📊 Real-time analytics

---

## 🏗️ Architecture

### Microservices

```
┌─────────────────────────────────────────────────────┐
│                   Load Balancer                      │
│                  (Nginx + SSL)                       │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
              ┌─────────────┐
              │ API Gateway │  ← Authentication
              └──────┬──────┘    Rate Limiting
                     │
      ┌──────────────┼──────────────┐
      │              │              │
      ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Catalog  │  │  Order   │  │ Payment  │
│ Service  │  │ Service  │  │ Service  │
│  :8082   │  │  :8083   │  │  :8084   │
└──────────┘  └──────────┘  └──────────┘
      │              │              │
      └──────────────┼──────────────┘
                     │
                     ▼
            ┌────────────────┐
            │   PostgreSQL   │
            │   (Primary)    │
            └────────────────┘
```

### Services

| Service | Port | Purpose |
|---------|------|---------|
| **API Gateway** | 8080 | Request routing, auth |
| **Auth Service** | 8081 | Authentication, JWT |
| **Catalog Service** | 8082 | Product management |
| **Order Service** | 8083 | Order processing |
| **Payment Service** | 8084 | Invoice, payments |
| **User Service** | 8085 | Company profiles |
| **Notification Service** | 8086 | Email, SMS |
| **Support Service** | 8087 | AI support assistant |
| **Team Service** | 8088 | Dev team assistant |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **PostgreSQL 15** or higher
- **Redis 7** or higher
- **Node.js 20** or higher (frontend)
- **Docker & Docker Compose**

### Local Development

```bash
# 1. Clone repository
git clone https://github.com/company/webshop.git
cd webshop

# 2. Start infrastructure
docker-compose up -d postgres redis rabbitmq

# 3. Start backend services
cd backend/catalog-service
./mvnw spring-boot:run

# 4. Start frontend
cd frontend
npm install
npm run dev
```

**URLs:**
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

---

## 📚 Documentation

### For Developers

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture overview
- **[API.md](docs/API.md)** - REST API documentation
- **[DATABASE.md](docs/DATABASE.md)** - Database schema and optimization
- **[TECHNICAL_DEBT.md](docs/TECHNICAL_DEBT.md)** - Known issues and improvements
- **[DEPLOYMENT.md](docs/DEPLOYMENT.md)** - Deployment procedures

### For Operations

- **[MONITORING.md](docs/MONITORING.md)** - Monitoring and alerting
- **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** - Common issues
- **[RUNBOOK.md](docs/RUNBOOK.md)** - Operational procedures

### For Product

- **[USER_GUIDE.md](docs/USER_GUIDE.md)** - End-user documentation
- **[FEATURES.md](docs/FEATURES.md)** - Feature specifications
- **[ROADMAP.md](docs/ROADMAP.md)** - Product roadmap

---

## 🛠️ Technology Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.0
- **Database:** PostgreSQL 15
- **Cache:** Redis 7
- **Message Queue:** RabbitMQ 3.12
- **Search:** Elasticsearch 8
- **API:** REST + OpenAPI 3.0

### Frontend
- **Framework:** Vue 3 (Composition API)
- **Language:** TypeScript 5
- **Build:** Vite 5
- **UI:** Element Plus
- **State:** Pinia

### DevOps
- **Cloud:** AWS (EKS, RDS, ElastiCache)
- **Containers:** Docker + Kubernetes
- **CI/CD:** GitHub Actions
- **Monitoring:** Prometheus + Grafana
- **Logging:** ELK Stack

### AI/ML
- **LLM:** OpenRouter (Claude 3.5 Sonnet)
- **RAG:** Custom with pgvector
- **MCP:** Model Context Protocol
- **Embeddings:** text-embedding-3-large

---

## 🔐 Security

### Authentication
- JWT tokens (1 hour expiry)
- Refresh tokens (30 days)
- OAuth2 integration (Google, Microsoft)

### Authorization
- Role-Based Access Control (RBAC)
- Principle of least privilege
- MFA for admin accounts

### Compliance
- **PCI DSS:** Payment card data compliance
- **GDPR:** Data privacy compliance
- **ISO 27001:** Information security

---

## 📊 Performance

### Current Metrics
- **Uptime:** 99.95%
- **Response Time:** < 200ms (p95)
- **Throughput:** 10,000 orders/day
- **Concurrent Users:** 1,000+

### Optimization
- **Caching:** Redis (15 min TTL)
- **CDN:** CloudFront
- **Database:** Connection pooling, indexes
- **API:** Pagination, compression

---

## 🧪 Testing

### Test Coverage
- **Unit Tests:** 75%
- **Integration Tests:** 60%
- **E2E Tests:** Manual

### Running Tests

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Code coverage
./mvnw jacoco:report
```

---

## 🚢 Deployment

### Environments

| Environment | Branch | URL |
|-------------|--------|-----|
| **Development** | feature/* | http://localhost:8080 |
| **Staging** | develop | https://staging.webshop.example.com |
| **Production** | main | https://webshop.example.com |

### Deployment Process

```bash
# 1. Create release tag
git tag -a v2.1.0 -m "Release 2.1.0"
git push origin v2.1.0

# 2. GitHub Actions automatically:
#    - Runs tests
#    - Builds Docker images
#    - Deploys to production

# 3. Monitor deployment
kubectl get pods
kubectl logs -f deployment/catalog-service
```

See [DEPLOYMENT.md](docs/DEPLOYMENT.md) for details.

---

## 📈 Monitoring

### Dashboards
- **Grafana:** https://grafana.webshop.example.com
- **Kibana:** https://kibana.webshop.example.com
- **Jaeger:** https://jaeger.webshop.example.com

### Key Metrics
- Request rate (req/s)
- Error rate (%)
- Response time (p50, p95, p99)
- Database connections
- Cache hit ratio

### Alerts
- High error rate (> 1%)
- Slow response time (> 1s)
- Database connection pool exhausted
- Service down

---

## 🤝 Contributing

### Development Workflow

1. **Create feature branch**
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Make changes and commit**
   ```bash
   git commit -m "feat: add new feature"
   ```

3. **Push and create PR**
   ```bash
   git push origin feature/new-feature
   ```

4. **Code review** (2 approvals required)

5. **Merge to develop**

### Commit Message Format

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Maintenance

---

## 📞 Support

### For Developers
- **Slack:** #engineering
- **Email:** dev@webshop.example.com

### For Operations
- **On-Call:** +49 XXX XXXXXXX
- **Slack:** #ops-alerts

### For Business
- **Product Team:** product@webshop.example.com
- **Sales:** sales@webshop.example.com

---

## 📋 Known Issues

See [TECHNICAL_DEBT.md](docs/TECHNICAL_DEBT.md) for current technical debt and known issues.

**Critical Issues:**
- Payment retry logic needs improvement (TASK-234)
- Database connection pool optimization (TASK-240)
- JWT secret in environment variable (TASK-245)

---

## 🗺️ Roadmap

### Q1 2026
- [ ] Implement event sourcing
- [ ] Add GraphQL API
- [ ] Migrate to Kubernetes
- [ ] API rate limiting

### Q2 2026
- [ ] Separate databases per service
- [ ] Implement CQRS
- [ ] Real-time analytics
- [ ] Mobile app (React Native)

### Q3 2026
- [ ] Machine learning recommendations
- [ ] Advanced analytics
- [ ] Multi-language support
- [ ] Global expansion

---

## 📄 License

Proprietary - All Rights Reserved

Copyright © 2026 WebShop GmbH

---

## 👥 Team

**Engineering:**
- Tech Lead: Alice Schmidt
- Backend: Bob Mueller, Charlie Weber
- Frontend: Diana Fischer, Erik Becker
- DevOps: Frank Wagner

**Product:**
- Product Manager: Greta Hoffmann
- Product Designer: Hans Schulz

**Operations:**
- Site Reliability: Iris Klein
- Support Lead: Julia Koch

---

## 📅 Changelog

### v2.1.0 (2026-01-20)
- feat: AI-powered support assistant
- feat: Team assistant with task management
- fix: Payment retry improvements
- perf: Database query optimization

### v2.0.0 (2025-12-15)
- feat: Complete redesign with Vue 3
- feat: Microservices architecture
- feat: Kubernetes deployment
- breaking: API v2

### v1.5.0 (2025-09-01)
- feat: Multi-tier pricing
- feat: EDO integration
- fix: Order processing bugs

See [CHANGELOG.md](CHANGELOG.md) for full history.

---

**Built with ❤️ by the WebShop Team**

Last Updated: 2026-01-15