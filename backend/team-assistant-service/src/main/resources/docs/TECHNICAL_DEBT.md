# B2B WebShop - Technical Debt

> Last Updated: 2026-01-15  
> Priority: High (🔴) | Medium (🟡) | Low (🟢)

## Overview

This document tracks known technical debt, architectural issues, and improvement opportunities in the B2B WebShop system.

---

## Critical Issues 🔴

### 1. Payment Service - Retry Logic

**Issue:** Payment retry logic is not robust. If external gateway fails, transaction is lost.

**Impact:**
- Lost revenue (payment failures)
- Manual reconciliation required
- Customer complaints

**Current Implementation:**
```java
// PaymentService.java
public PaymentResult processPayment(PaymentRequest request) {
    try {
        return stripeGateway.charge(request);
    } catch (StripeException e) {
        log.error("Payment failed", e);
        return PaymentResult.failed();
    }
}
```

**Recommended Solution:**
- Implement exponential backoff retry (3 attempts)
- Add payment queue (RabbitMQ)
- Implement idempotency keys
- Add webhook handling for async payment confirmation

**Estimated Effort:** 3-5 days  
**Assigned To:** Backend Team  
**Target:** Q1 2026  
**Related Task:** TASK-234

---

### 2. Database Connection Pooling

**Issue:** Connection pool exhaustion during peak load.

**Symptoms:**
- `java.sql.SQLException: Connection pool exhausted`
- Response time spikes > 5s
- Service degradation at 500+ concurrent users

**Root Cause:**
- HikariCP pool size too small (10 connections)
- Long-running queries holding connections
- No connection timeout configured

**Solution:**
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Additional Actions:**
- Add connection pool monitoring
- Optimize slow queries (see DATABASE.md)
- Implement query timeout (5s)

**Estimated Effort:** 1-2 days  
**Status:** In Progress  
**Related Task:** TASK-240

---

### 3. Security - JWT Secret in Code

**Issue:** JWT secret key hardcoded in source code.

**Current:**
```java
// SecurityConfig.java
private static final String JWT_SECRET = "hardcoded-secret-key-DO-NOT-USE";
```

**Risk:**
- Secret exposed in Git history
- Cannot rotate keys without code deploy
- Security vulnerability if leaked

**Solution:**
- Move to environment variable: `JWT_SECRET`
- Use AWS Secrets Manager / HashiCorp Vault
- Implement key rotation mechanism

**Estimated Effort:** 1 day  
**Priority:** CRITICAL  
**Related Task:** TASK-245

---

## High Priority Issues 🟡

### 4. Order Service - N+1 Query Problem

**Issue:** Fetching orders with items causes N+1 queries.

**Performance Impact:**
- Loading 100 orders = 1 + 100 queries (101 total)
- Response time: 2-3s (should be < 500ms)

**Current Code:**
```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems(); // Lazy loading triggers query
}
```

**Solution:**
```java
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
List<Order> findAllWithItems();
```

**Estimated Effort:** 2-3 days (update all queries)  
**Impact:** 70% reduction in database queries  
**Related Task:** TASK-250

---

### 5. Catalog Service - Missing Caching

**Issue:** Product data fetched from database on every request.

**Performance:**
- Database load: 1000+ queries/minute
- Response time: 200-300ms (could be < 50ms)

**Solution:**
```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(String id) {
    return productRepository.findById(id);
}
```

**Cache Strategy:**
- Product data: 15 min TTL
- Categories: 1 hour TTL
- Prices: 5 min TTL

**Estimated Effort:** 3 days  
**Related Task:** TASK-255

---

### 6. Missing API Rate Limiting

**Issue:** No rate limiting on public API endpoints.

**Risk:**
- API abuse / DDoS vulnerability
- Resource exhaustion
- Unfair resource usage

**Solution:**
- Implement rate limiting per customer tier
  - Bronze: 100 req/hour
  - Silver: 500 req/hour
  - Gold: 1000 req/hour
  - Platinum: 5000 req/hour
- Use Redis for distributed rate limiting
- Return 429 Too Many Requests

**Estimated Effort:** 2-3 days  
**Related Task:** TASK-260

---

### 7. Frontend - Bundle Size Too Large

**Issue:** Initial bundle size: 2.5 MB (gzipped: 800 KB)

**Impact:**
- Slow page load on mobile (3-5s)
- High bandwidth usage
- Poor SEO score

**Analysis:**
- Moment.js: 300 KB (should use date-fns)
- Lodash: 200 KB (import specific functions)
- Unused CSS: 150 KB

**Solution:**
- Code splitting by route
- Lazy load components
- Tree shaking
- Replace heavy libraries

**Target:** < 500 KB (gzipped < 200 KB)  
**Estimated Effort:** 3-5 days  
**Related Task:** TASK-265

---

## Medium Priority 🟢

### 8. Missing Integration Tests

**Issue:** Only unit tests exist, no integration tests.

**Coverage:**
- Unit tests: 75%
- Integration tests: 0%
- E2E tests: Manual only

**Risks:**
- Regressions not caught
- Breaking changes in APIs
- Database schema mismatches

**Solution:**
- Add Testcontainers for DB
- Test REST endpoints
- Test service interactions
- Add to CI/CD pipeline

**Estimated Effort:** 2 weeks  
**Related Task:** TASK-270

---

### 9. Inconsistent Error Handling

**Issue:** Each service has different error response format.

**Example Problems:**
```json
// Catalog Service
{"error": "Product not found"}

// Order Service
{"errorCode": "NOT_FOUND", "message": "..."}

// Payment Service
{"status": "error", "details": {...}}
```

**Solution:**
- Standardize error format (see API.md)
- Implement global exception handler
- Add error codes enum

**Estimated Effort:** 3-4 days  
**Related Task:** TASK-275

---

### 10. Logging - Too Much Noise

**Issue:** Logs contain too much DEBUG/INFO, hard to find errors.

**Problems:**
- 1 GB logs per day per service
- Signal-to-noise ratio too low
- Difficult troubleshooting

**Solution:**
```yaml
# application.yml
logging:
  level:
    root: WARN
    de.jivz: INFO
    de.jivz.orderservice: DEBUG  # Only for specific services
```

**Best Practices:**
- ERROR: Production issues only
- WARN: Potential issues
- INFO: Important business events
- DEBUG: Development only

**Estimated Effort:** 1 day  
**Related Task:** TASK-280

---

### 11. Missing Health Checks

**Issue:** Services don't report health status properly.

**Current:**
```java
@GetMapping("/health")
public String health() {
    return "OK";
}
```

**Problems:**
- Doesn't check database connectivity
- Doesn't check external services
- No detailed status information

**Solution:**
```java
@GetMapping("/health")
public HealthStatus health() {
    return HealthStatus.builder()
        .database(checkDatabase())
        .redis(checkRedis())
        .externalApi(checkStripe())
        .build();
}
```

**Estimated Effort:** 2 days  
**Related Task:** TASK-285

---

### 12. Documentation Out of Date

**Issue:** API documentation doesn't match actual implementation.

**Examples:**
- Missing new endpoints
- Deprecated endpoints still documented
- Wrong request/response examples

**Solution:**
- Generate docs from OpenAPI annotations
- Add integration tests for examples
- Automate doc generation in CI/CD

**Estimated Effort:** 3 days  
**Related Task:** TASK-290

---

## Architectural Improvements

### 13. Microservices Communication

**Current:** Direct REST calls between services

**Issues:**
- Tight coupling
- No retry mechanism
- Synchronous (blocking)
- Single point of failure

**Proposed Solution:**
- Implement event-driven architecture
- Use RabbitMQ for async communication
- Add circuit breaker pattern (Resilience4j)

**Benefits:**
- Loose coupling
- Better scalability
- Fault tolerance
- Event sourcing capability

**Estimated Effort:** 4-6 weeks  
**Priority:** Long-term

---

### 14. Observability

**Current State:**
- Basic logging (ELK)
- No distributed tracing
- Limited metrics

**Missing:**
- Request tracing across services
- Performance bottleneck identification
- Service dependency mapping

**Proposed Solution:**
- Implement OpenTelemetry
- Add Jaeger for tracing
- Enhance Prometheus metrics
- Create Grafana dashboards

**Estimated Effort:** 2-3 weeks  
**Priority:** Q2 2026

---

### 15. Database Sharding

**Current:** Single PostgreSQL instance

**Limitations:**
- Max throughput: ~10K TPS
- Single point of failure
- Difficult to scale writes

**Future Consideration:**
- Shard by company_id
- Implement read replicas
- Consider PostgreSQL partitioning

**Note:** Not urgent until 50K+ companies  
**Estimated Effort:** 6-8 weeks  
**Priority:** Q3 2026

---

## Quick Wins (< 1 day effort)

### 16. Add Database Indexes

Missing indexes causing slow queries:
```sql
CREATE INDEX idx_orders_status_created ON orders(status, created_at);
CREATE INDEX idx_order_items_product_order ON order_items(product_id, order_id);
CREATE INDEX idx_invoices_status_due_date ON invoices(status, due_date);
```

**Impact:** 30-50% query performance improvement  
**Effort:** 2 hours  
**Related Task:** TASK-295

---

### 17. Enable Gzip Compression

```yaml
# application.yml
server:
  compression:
    enabled: true
    mime-types: application/json,text/html,text/css,application/javascript
```

**Impact:** 70% reduction in bandwidth  
**Effort:** 15 minutes  
**Related Task:** TASK-300

---

### 18. Add Request Timeout

```yaml
spring:
  mvc:
    async:
      request-timeout: 30000  # 30 seconds
```

**Prevents:** Hanging requests  
**Effort:** 10 minutes  
**Related Task:** TASK-305

---

## Refactoring Opportunities

### 19. Extract Common Code

**Problem:** Duplicate code across services

**Examples:**
- JWT validation logic (duplicated 5x)
- Error handling (duplicated 8x)
- Pagination logic (duplicated 6x)

**Solution:**
- Create shared library module
- Extract common utilities
- Reduce duplication

**Estimated Effort:** 1-2 weeks

---

### 20. Modernize Legacy Code

**Issues:**
- Old Spring Boot 2.x code patterns
- Imperative style (should be functional)
- Verbose error handling

**Example Refactoring:**
```java
// Old
Optional<Product> product = productRepository.findById(id);
if (product.isPresent()) {
    return product.get();
} else {
    throw new NotFoundException("Product not found");
}

// New
return productRepository.findById(id)
    .orElseThrow(() -> new NotFoundException("Product not found"));
```

**Estimated Effort:** Ongoing (per service)

---

## Prioritization Matrix

| Issue | Priority | Impact | Effort | Status |
|-------|----------|--------|--------|--------|
| Payment Retry | 🔴 | High | 5d | Planned |
| Connection Pool | 🔴 | High | 2d | In Progress |
| JWT Secret | 🔴 | Critical | 1d | Not Started |
| N+1 Queries | 🟡 | High | 3d | Planned |
| Caching | 🟡 | Medium | 3d | Not Started |
| Rate Limiting | 🟡 | Medium | 3d | Not Started |
| Bundle Size | 🟡 | Medium | 5d | Not Started |
| Integration Tests | 🟢 | Medium | 10d | Planned |
| Error Handling | 🟢 | Low | 4d | Not Started |

---

## Review Process

Technical debt is reviewed:
- **Weekly:** In team standup (new issues)
- **Monthly:** Priority reassessment
- **Quarterly:** Architecture review

---

## Contributing

To add new technical debt:
1. Create JIRA ticket
2. Add to this document
3. Assess priority and effort
4. Discuss in team meeting

---

## Contact

- **Tech Lead:** tech-lead@webshop.example.com
- **Questions:** #tech-debt on Slack

**Last Updated:** 2026-01-15
