# Azure VM Deployment Plan

This document is the deployment plan for the four Spring Boot services and their supporting infrastructure.

## Target architecture

The first Azure deployment will use one Linux VM running Docker Compose:

```text
Azure Linux VM
└── Docker Compose
    ├── user-service
    ├── order-service
    ├── inventory-service
    ├── notification-service
    ├── PostgreSQL
    ├── Kafka
    └── Couchbase
```

This is a development/demo deployment, not a highly available production architecture. A later migration can move the stateless Spring Boot services to Azure Container Apps and the stateful components to managed services.

## Deployment phases

### Phase 1 - Prepare the repositories

- Add a Java 21 multi-stage Dockerfile to each service.
- Add `.dockerignore` files so Maven build output and local tooling are not copied into images.
- Keep production configuration environment-driven; do not use the `dev` profile in Azure.
- Add health endpoints using Spring Boot Actuator.
- Add a checked-in `.env.example` without real secrets.

**Current status:** Dockerfiles and service-level `.dockerignore` files are present. The images build the service JAR in a Maven build stage and run it as a non-root user in a Java 21 runtime image. The complete Compose wiring is now present; local execution still requires Docker Desktop or another Docker Engine.

### Phase 2 - Build the complete Compose deployment

- Add all four Spring Boot services to Compose. **Done.**
- Put every container on one private Docker network. Compose provides this default network. **Done.**
- Inject production environment variables into the services. **Done; see `.env.example`.**
- Use Compose service names instead of `localhost` for container-to-container communication. **Done.**
- Change Kafka's advertised listener from `localhost:9092` to its internal Compose name. **Done.**
- Add health checks and startup dependencies. Infrastructure health checks and dependency ordering are present; application Actuator health checks remain a later step.
- Add restart policies and Docker log rotation. Restart policies are present; log rotation will be added with the operational hardening step.

Expected internal endpoints include:

```text
PostgreSQL:       postgres:5432
Kafka:            kafka:9092
Couchbase:        couchbase:8091
Inventory API:    inventory-service:8081
```

### Phase 3 - Make infrastructure persistent and repeatable

- Add named volumes for PostgreSQL, Kafka, and Couchbase. **Done.**
- Add an idempotent Couchbase initialization script. **Done: `deployment/couchbase-init.sh`.**
- Create the `inventory` bucket, `Inventory` scope, `InventoryList` collection, and required index. **Done by the Couchbase init container.**
- Provision Kafka topics `InventoryUpdate` and `NotificationTrigger`. **Done by `deployment/kafka-init.sh`.**
- Create the PostgreSQL database from `POSTGRES_DB` and service schemas from `deployment/postgres-init/01-schemas.sql`. Flyway still applies the service tables.
- Document backup and restore commands.

### Phase 4 - Validate locally

Install Docker Desktop with the WSL2 backend, then run the same deployment stack locally before using Azure:

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
docker compose logs -f
```

Run the API flow tests from `flow-test-curls.md`. Verify that migrations run, the services can communicate, Kafka consumers receive events, and the Couchbase read model is updated.

If Docker Desktop is not available, the same commands can be run on a temporary Linux host or on the Azure VM, but local validation is preferred.

### Phase 5 - Create the Azure VM

1. Create an Azure resource group and select a nearby region.
2. Create an Ubuntu Linux VM with SSH key authentication.
3. Start with at least 2 vCPU and 8 GB RAM for the complete demo stack.
4. Use a persistent Standard SSD disk.
5. Enable automatic shutdown to control cost.
6. Open only SSH and the required API ports in the Network Security Group.
7. Install Docker Engine and the Docker Compose plugin.
8. Clone the repository and create a protected `.env` file on the VM.
9. Start the stack with Docker Compose.
10. Verify container health and API flows.

The VM is administered over SSH. It does not require a graphical desktop:

```bash
ssh azureuser@<vm-public-ip>
```

### Phase 6 - Database access

PostgreSQL and Couchbase should remain private by default. Use SSH port forwarding from a development computer when database tools need access.

PostgreSQL example:

```bash
ssh -L 5432:localhost:5432 azureuser@<vm-public-ip>
```

Do not expose PostgreSQL, Kafka, or Couchbase to `0.0.0.0/0`. If public access is ever required, restrict the Azure Network Security Group to a known source IP, use strong credentials, and enable TLS where supported.

### Phase 7 - Logs and monitoring

Initial VM troubleshooting uses Docker logs:

```bash
docker compose logs -f inventory-service
docker compose logs -f kafka
docker stats
```

After the deployment works, add Azure Monitor and a Log Analytics Workspace for centralized VM/container logs and alerts. Application Insights can later provide request, exception, and dependency tracing for the Spring Boot services.

### Phase 8 - Backups and operations

Before storing important data:

- Schedule PostgreSQL logical backups.
- Document Couchbase backup/export steps.
- Keep Docker volumes on persistent disks.
- Configure log rotation.
- Monitor disk, memory, CPU, and container restarts.
- Keep a VM recreation checklist.
- Use Azure budget alerts and automatic shutdown.

## Environment contract

The production applications require these values:

```text
AZURE_POSTGRES_URL
AZURE_DB_USERNAME
AZURE_DB_PASSWORD
AZURE_KAFKA_BOOTSTRAP_SERVERS
COUCHBASE_CONNECTION_STRING
COUCHBASE_USERNAME
COUCHBASE_PASSWORD
JWT_SECRET
AZURE_INVENTORY_SERVICE_URL
INVENTORY_QUERY_BASE_URL
```

The Compose deployment will use container DNS names for these values. Real secrets belong in a protected VM `.env` file or Azure Key Vault, never in Git.

## Known limitations

- One VM is a single point of failure.
- The initial Kafka setup is a single broker and is not highly available.
- A single Couchbase instance is not highly available.
- A B-series VM may throttle sustained CPU usage after its burst credits are consumed.
- A non-burstable VM is more predictable but costs more.
- Database exposure must be deliberately configured; private networking and SSH tunnels are the default.
