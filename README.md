# Tala Backend V2

AI-first baby care assistant backend - Clean slate rewrite optimized for Apple Silicon

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────┐
│              API Gateway (8080)                      │
└────────┬─────────┬─────────┬──────────┬─────────────┘
         │         │         │          │
    ┌────▼───┐ ┌──▼────┐ ┌──▼────┐ ┌──▼────┐
    │ Event  │ │ Query │ │  AI   │ │ User  │
    │Service │ │Service│ │Service│ │Service│
    │ (8081) │ │(8082) │ │(8083) │ │(8084) │
    └────┬───┘ └──┬────┘ └──┬────┘ └──┬────┘
         │        │         │         │
    ┌────▼────────▼─────────▼─────────▼────┐
    │         PostgreSQL (5432)             │
    │         ClickHouse (8123)             │
    │         Kafka (9092)                  │
    │         Redis (6379)                  │
    └──────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** (Temurin recommended for ARM64)
- **Docker Desktop** for Mac (Apple Silicon)
- **Maven 3.9+**
- **Git**

### Installation

1. **Clone the repository**
   ```bash
   cd backend
   ```

2. **Set up environment**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start infrastructure**
   ```bash
   # Minimal mode (PostgreSQL + Redis only) - Recommended for daily development
   ./scripts/start-dev.sh
   
   # Or choose a specific mode:
   ./scripts/start-dev.sh --analytics   # + ClickHouse
   ./scripts/start-dev.sh --events      # + Kafka + Zookeeper
   ./scripts/start-dev.sh --monitoring  # + Prometheus + Grafana
   ./scripts/start-dev.sh --full        # All services
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run a service**
   ```bash
   cd services/user-service
   mvn spring-boot:run
   ```

## 🐳 Docker Setup

### Development Environment Modes

The development environment supports **4 modes** to optimize resource usage:

#### Minimal Mode (Default) - Recommended for Daily Development
```bash
./scripts/start-dev.sh
# Starts: PostgreSQL + Redis only
# Resources: ~1 CPU core, ~768MB RAM
# Use case: Most feature development
```

#### Analytics Mode
```bash
./scripts/start-dev.sh --analytics
# Adds: ClickHouse
# Resources: ~3 CPU cores, ~1.5GB RAM
# Use case: Testing analytics queries
```

#### Events Mode
```bash
./scripts/start-dev.sh --events
# Adds: Kafka + Zookeeper
# Resources: ~2.5 CPU cores, ~1.5GB RAM
# Use case: Testing event streaming
```

#### Monitoring Mode
```bash
./scripts/start-dev.sh --monitoring
# Adds: Prometheus + Grafana
# Resources: ~1.5 CPU cores, ~1GB RAM
# Use case: Testing metrics and dashboards
```

#### Full Mode
```bash
./scripts/start-dev.sh --full
# Starts: All services
# Resources: ~5.5 CPU cores, ~3.5GB RAM
# Use case: Pre-deployment testing
```

### Common Commands

```bash
# Stop all services
./scripts/stop-dev.sh

# View logs
docker-compose logs -f

# Clean up (⚠️ deletes all data)
docker-compose down -v
```

### Production Deployment (Mac Mini)

```bash
# Prepare production environment
cp .env.production.example .env.production
# Edit .env.production with secure credentials

# Deploy to production
./scripts/deploy-prod.sh

# Backup data
./scripts/backup.sh
```

## 📊 Services

| Service | Port | Description |
|---------|------|-------------|
| **Gateway** | 8080 | API Gateway, routing, authentication |
| **Event Service** | 8081 | Event CRUD, Kafka producer |
| **Query Service** | 8082 | Analytics queries, ClickHouse |
| **AI Service** | 8083 | Pattern detection, insights |
| **User Service** | 8084 | User & profile management |
| **PostgreSQL** | 5432 | Operational database |
| **ClickHouse** | 8123 | Analytics database |
| **Kafka** | 9092 | Event streaming |
| **Redis** | 6379 | Caching layer |
| **Prometheus** | 9090 | Metrics collection |
| **Grafana** | 3000 | Metrics visualization |

## 🔧 Development

### Project Structure

```
backend/
├── shared/                    # Shared libraries
│   ├── common-core/          # Core utilities, base classes
│   ├── common-kafka/         # Kafka producers/consumers
│   └── common-clickhouse/    # ClickHouse utilities
├── services/                 # Microservices
│   ├── origin-data-service/  # Event sourcing & original data
│   ├── query-service/        # Analytics queries
│   ├── ai-service/           # AI/ML features
│   ├── user-service/         # User management
│   └── gateway-service/      # API Gateway
├── infrastructure/           # Infrastructure configs
│   ├── postgresql/          # PostgreSQL init scripts
│   ├── clickhouse/          # ClickHouse configs
│   └── monitoring/          # Prometheus, Grafana
├── scripts/                 # Deployment scripts
└── .github/workflows/       # CI/CD pipelines
```

### Building Modules

```bash
# Build all modules
mvn clean install

# Build specific module
cd shared/common-core
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run tests only
mvn test

# Generate coverage report
mvn jacoco:report
```

### Running Services

```bash
# Run with Maven
cd services/user-service
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debugging
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EventServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## 🔒 Security

### Environment Variables

**Never commit sensitive data!** Use environment variables:

```bash
# Development
.env              # Local development (git-ignored)

# Production
.env.production   # Production (git-ignored)
```

### GitHub Secrets

Configure these secrets in GitHub repository settings:

- `SSH_PRIVATE_KEY` - SSH key for Mac Mini access
- `SERVER_HOST` - Mac Mini IP or domain
- `SERVER_USER` - Server username
- `SLACK_WEBHOOK` - Slack notifications (optional)

## 🚢 Deployment

### CI/CD Pipeline

GitHub Actions automatically:
1. ✅ Runs tests on every PR
2. 🔨 Builds Docker images on main branch
3. 🚀 Deploys to Mac Mini on main branch
4. 🔍 Runs security scans

### Manual Deployment

```bash
# Build and push images
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml push

# Deploy on Mac Mini
ssh user@mac-mini
cd ~/tala-backend
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

## 🍎 Apple Silicon Optimization

All Docker images use `platform: linux/arm64` for optimal performance on:
- MacBook Air (Development)
- Mac Mini (Production)

### Resource Limits

**Development** (MacBook Air):
- CPU: 0.5-1.0 cores per service
- Memory: 256-512MB per service

**Production** (Mac Mini):
- CPU: 1.0-2.0 cores per service
- Memory: 512MB-1GB per service

## 📊 Monitoring

### Prometheus Metrics

Access: http://localhost:9090

**Key Metrics:**
- `http_server_requests_seconds` - Request latency
- `jvm_memory_used_bytes` - Memory usage
- `system_cpu_usage` - CPU usage

### Grafana Dashboards

Access: http://localhost:3000 (admin/admin)

**Pre-configured Dashboards:**
- Application Overview
- JVM Metrics
- Database Performance
- Request Analytics

## 🐛 Troubleshooting

### Docker Issues

```bash
# Reset Docker
./scripts/stop-dev.sh
docker-compose down -v
./scripts/start-dev.sh

# View logs
docker-compose logs -f [service-name]

# Check service health
docker ps
docker stats
```

### Build Issues

```bash
# Clean Maven cache
mvn clean
rm -rf ~/.m2/repository/com/tala

# Update dependencies
mvn clean install -U

# Check Java version
java -version  # Should be 21
```

### Database Issues

```bash
# Connect to PostgreSQL
docker exec -it tala-postgres-dev psql -U tala -d tala_db

# Connect to ClickHouse
docker exec -it tala-clickhouse-dev clickhouse-client

# Reset databases
docker-compose down -v
docker-compose up -d
```

## 📚 API Documentation

### Origin Data Service & Timeline

```bash
# Get timeline entries for a profile (paginated)
GET /api/v1/timeline/profile/{profileId}?page=0&size=20

# Get timeline entries for a profile in a time range
GET /api/v1/timeline/profile/{profileId}/range?startTime=...&endTime=...

# Get a single timeline entry
GET /api/v1/timeline/{id}
```

Full API documentation: [API_SPEC.md](./docs/API_SPEC.md)

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Run tests: `mvn test`
4. Commit with conventional commits
5. Create a pull request

### Commit Convention

```
feat(origin-data-service): add timeline range API
fix(query-service): resolve timeout issue
docs: update API documentation
test(ai-service): add pattern detection tests
chore(deps): update spring boot to 3.2.2
```

## 📝 Documentation

- [Implementation Plan](../docs/BACKEND-V2-IMPLEMENTATION-PLAN.md)
- [Week 2-10 Plan](../docs/BACKEND-V2-WEEK-2-10-PLAN.md)
- [Quick Start Guide](../docs/NEW-BACKEND-QUICK-START.md)

## 📄 License

MIT License - See [LICENSE](../LICENSE) for details

## 🆘 Support

- Create an issue on GitHub
- Check existing documentation
- Review Docker logs
- Contact the team

---

**Built with ❤️ for Apple Silicon**
