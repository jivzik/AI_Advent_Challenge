# B2B WebShop - Deployment Guide

> Last Updated: 2026-01-15  
> Environment: AWS (eu-central-1)

## Table of Contents
1. [Quick Start](#quick-start)
2. [Environments](#environments)
3. [CI/CD Pipeline](#cicd-pipeline)
4. [Deployment Procedures](#deployment-procedures)
5. [Rollback Procedures](#rollback-procedures)
6. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
- AWS CLI configured
- Docker installed
- kubectl configured for EKS
- GitHub access

### Deploy to Production

```bash
# 1. Tag release
git tag -a v2.1.0 -m "Release 2.1.0"
git push origin v2.1.0

# 2. GitHub Actions automatically:
#    - Runs tests
#    - Builds Docker images
#    - Pushes to ECR
#    - Updates ECS/EKS
#    - Runs smoke tests

# 3. Monitor deployment
./scripts/deployment-status.sh
```

---

## Environments

### Development
- **Purpose:** Local development
- **Infrastructure:** Docker Compose
- **Database:** PostgreSQL (local)
- **URL:** http://localhost:8080

### Staging
- **Purpose:** Pre-production testing
- **Infrastructure:** AWS EKS
- **Database:** RDS (r6g.large)
- **URL:** https://staging.webshop.example.com
- **Deploy:** On merge to `develop` branch

### Production
- **Purpose:** Live customer traffic
- **Infrastructure:** AWS EKS + ECS
- **Database:** RDS Multi-AZ (r6g.xlarge)
- **URL:** https://webshop.example.com
- **Deploy:** On tag push (v*)

---

## Infrastructure

### AWS Resources

**Compute:**
- EKS Cluster: `webshop-prod-eks`
- Node Groups: 3 x t3.xlarge (Auto Scaling: 3-10)
- ECS Fargate: For batch jobs

**Database:**
- RDS PostgreSQL 15.4
- Instance: r6g.xlarge
- Multi-AZ: Enabled
- Read Replicas: 2

**Caching:**
- ElastiCache Redis 7.0
- Node: cache.r6g.large
- Cluster Mode: Enabled

**Load Balancing:**
- Application Load Balancer (ALB)
- SSL: ACM Certificate
- WAF: Enabled

**Storage:**
- S3: Product images, invoices
- EFS: Shared file storage

**Networking:**
- VPC: 10.0.0.0/16
- Public Subnets: 3 AZs
- Private Subnets: 3 AZs
- NAT Gateways: 3

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run Tests
        run: ./mvnw test
      
      - name: Code Coverage
        run: ./mvnw jacoco:report

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker Image
        run: docker build -t webshop/catalog-service:${{ github.ref_name }} .
      
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
          docker push $ECR_REGISTRY/catalog-service:${{ github.ref_name }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EKS
        run: |
          kubectl set image deployment/catalog-service \
            catalog-service=$ECR_REGISTRY/catalog-service:${{ github.ref_name }}
      
      - name: Wait for Rollout
        run: kubectl rollout status deployment/catalog-service
      
      - name: Smoke Tests
        run: ./scripts/smoke-tests.sh
```

### Pipeline Stages

1. **Test** (5 min)
    - Unit tests
    - Integration tests
    - Code coverage check (> 70%)

2. **Build** (10 min)
    - Compile Java code
    - Build Docker images
    - Push to AWS ECR

3. **Deploy** (15 min)
    - Update Kubernetes deployments
    - Rolling update (zero downtime)
    - Health checks

4. **Verify** (5 min)
    - Smoke tests
    - Health endpoint checks
    - Basic functionality tests

**Total Time:** ~35 minutes

---

## Deployment Procedures

### 1. Regular Deployment (Automated)

**Trigger:** Push git tag `v*`

**Steps:**
```bash
# 1. Create and push tag
git tag -a v2.1.0 -m "Release 2.1.0 - Payment retry improvements"
git push origin v2.1.0

# 2. Monitor GitHub Actions
# https://github.com/company/webshop/actions

# 3. Verify deployment
curl https://webshop.example.com/health

# 4. Monitor logs
kubectl logs -f deployment/catalog-service
```

**Rollout Strategy:** Rolling update
- Max unavailable: 1 pod
- Max surge: 1 pod
- Health check grace period: 30s

### 2. Hotfix Deployment

**Scenario:** Critical production bug

**Steps:**
```bash
# 1. Create hotfix branch
git checkout -b hotfix/payment-fix

# 2. Fix and test locally
./mvnw test

# 3. Commit and push
git commit -m "Fix payment retry issue"
git push origin hotfix/payment-fix

# 4. Tag and deploy
git tag -a v2.0.1-hotfix -m "Hotfix: Payment retry"
git push origin v2.0.1-hotfix

# 5. Fast-track through pipeline
# Monitor deployment closely

# 6. Merge back to main
git checkout main
git merge hotfix/payment-fix
git push origin main
```

**Time:** 30-45 minutes (including testing)

### 3. Database Migration

**Scenario:** New database schema changes

**Steps:**
```bash
# 1. Test migration on staging
kubectl exec -it postgres-pod -- psql -U ai_user -d webshop < V8_migration.sql

# 2. Verify migration
psql -U ai_user -d webshop -c "\dt"

# 3. Backup production database
aws rds create-db-snapshot \
  --db-instance-identifier webshop-prod \
  --db-snapshot-identifier webshop-$(date +%Y%m%d)

# 4. Schedule maintenance window
# Notify users: "Maintenance 2:00 AM - 4:00 AM"

# 5. Run migration on production
# (Flyway runs automatically on deployment)

# 6. Verify and monitor
```

**Best Practices:**
- Always backup before migration
- Test on staging first
- Migrations should be backwards compatible
- Have rollback plan ready

### 4. Infrastructure Changes

**Scenario:** Add new AWS resources

**Steps:**
```bash
# 1. Update Terraform configuration
cd infrastructure/terraform
vim main.tf

# 2. Plan changes
terraform plan -out=tfplan

# 3. Review plan carefully
terraform show tfplan

# 4. Apply changes
terraform apply tfplan

# 5. Verify resources
aws eks describe-cluster --name webshop-prod-eks
```

---

## Rollback Procedures

### 1. Application Rollback

**Trigger:** Critical issue detected after deployment

**Quick Rollback:**
```bash
# 1. Rollback to previous version
kubectl rollout undo deployment/catalog-service

# 2. Verify rollback
kubectl rollout status deployment/catalog-service

# 3. Check health
curl https://webshop.example.com/health
```

**Rollback to Specific Version:**
```bash
# 1. List deployment history
kubectl rollout history deployment/catalog-service

# 2. Rollback to revision
kubectl rollout undo deployment/catalog-service --to-revision=5

# 3. Verify
kubectl get pods -l app=catalog-service
```

**Time:** 2-5 minutes

### 2. Database Rollback

**Scenario:** Migration caused issues

**Steps:**
```bash
# 1. Stop application
kubectl scale deployment/catalog-service --replicas=0

# 2. Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier webshop-prod-restored \
  --db-snapshot-identifier webshop-20260115

# 3. Wait for restore (30-60 min)
aws rds wait db-instance-available \
  --db-instance-identifier webshop-prod-restored

# 4. Update database endpoint
kubectl edit configmap database-config

# 5. Restart application
kubectl scale deployment/catalog-service --replicas=3
```

**Time:** 1-2 hours (depending on database size)

---

## Monitoring Deployment

### Health Checks

```bash
# Check all services
./scripts/health-check.sh

# Individual service
curl https://webshop.example.com/api/catalog/health
curl https://webshop.example.com/api/order/health
curl https://webshop.example.com/api/payment/health
```

### Logs

```bash
# Real-time logs
kubectl logs -f deployment/catalog-service

# Last 100 lines
kubectl logs --tail=100 deployment/catalog-service

# Logs from specific pod
kubectl logs catalog-service-7d9f8b6c4-xk2m9

# Logs from all pods
kubectl logs -l app=catalog-service --all-containers=true
```

### Metrics

**Grafana Dashboard:**
- https://grafana.webshop.example.com

**Key Metrics:**
- Request rate (req/s)
- Error rate (%)
- Response time (p50, p95, p99)
- CPU usage (%)
- Memory usage (%)
- Database connections

---

## Troubleshooting

### Issue: Pod Not Starting

```bash
# Check pod status
kubectl get pods

# Describe pod for events
kubectl describe pod catalog-service-7d9f8b6c4-xk2m9

# Common issues:
# - Image pull error: Check ECR permissions
# - CrashLoopBackOff: Check logs
# - Pending: Check resources (CPU/memory)
```

### Issue: High Error Rate

```bash
# 1. Check logs for errors
kubectl logs -l app=catalog-service | grep ERROR

# 2. Check external dependencies
curl https://api.stripe.com/health

# 3. Check database
psql -U ai_user -d webshop -c "SELECT 1"

# 4. Consider rollback if critical
kubectl rollout undo deployment/catalog-service
```

### Issue: Slow Response Time

```bash
# 1. Check pod resources
kubectl top pods

# 2. Scale horizontally if needed
kubectl scale deployment/catalog-service --replicas=6

# 3. Check database performance
psql -U ai_user -d webshop -c "SELECT * FROM pg_stat_activity"

# 4. Check cache hit rate
redis-cli info stats | grep hit_rate
```

---

## Security

### Secrets Management

```bash
# Secrets stored in AWS Secrets Manager
aws secretsmanager get-secret-value --secret-id prod/database/password

# Kubernetes secrets (auto-synced)
kubectl get secrets
kubectl describe secret database-credentials
```

### SSL/TLS

- Certificate: AWS Certificate Manager (ACM)
- Renewal: Automatic (60 days before expiry)
- Cipher Suites: TLS 1.3 only

### Access Control

- IAM Roles: Service-specific
- Kubernetes RBAC: Namespace-based
- Database: Least privilege principle

---

## Disaster Recovery

### RTO (Recovery Time Objective): 4 hours
### RPO (Recovery Point Objective): 5 minutes

**Backup Strategy:**
- Database: Automated snapshots (every 6 hours)
- Point-in-time recovery: 5-minute granularity
- Cross-region backup: Weekly to us-east-1

**Recovery Procedure:**
```bash
# 1. Restore database from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier webshop-prod-recovered \
  --db-snapshot-identifier webshop-20260115-0600

# 2. Restore application from last good version
kubectl set image deployment/catalog-service \
  catalog-service=webshop/catalog-service:v2.0.0

# 3. Verify functionality
./scripts/smoke-tests.sh
```

---

## Maintenance Windows

**Scheduled Maintenance:**
- **Weekly:** Sunday 02:00-04:00 CET
- **Monthly:** First Sunday 02:00-06:00 CET

**Procedure:**
1. Notify users 48 hours in advance
2. Enable maintenance mode
3. Perform updates
4. Run tests
5. Disable maintenance mode
6. Monitor for 1 hour

---

## Deployment Checklist

### Pre-Deployment
- [ ] All tests pass
- [ ] Code reviewed and approved
- [ ] Database migration tested on staging
- [ ] Backup created
- [ ] Stakeholders notified
- [ ] Rollback plan documented

### During Deployment
- [ ] Monitor GitHub Actions pipeline
- [ ] Watch for errors in logs
- [ ] Check health endpoints
- [ ] Monitor key metrics (Grafana)
- [ ] Run smoke tests

### Post-Deployment
- [ ] Verify all services healthy
- [ ] Check error rates
- [ ] Monitor for 30 minutes
- [ ] Update deployment docs
- [ ] Notify team of successful deployment
- [ ] Close deployment ticket

---

## Emergency Contacts

- **On-Call Engineer:** +49 XXX XXXXXXX
- **DevOps Lead:** devops@webshop.example.com
- **AWS Support:** Enterprise Support (24/7)
- **Incident Channel:** #incidents on Slack

---

## References

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

**Last Updated:** 2026-01-15