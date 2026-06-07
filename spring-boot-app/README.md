### Contents

This repository contains a spring boot microservices application implemented using the latest versions of Spring framework APIs. The implementation is based on the spring framework, boot3.0+, kafka, mongodb, mysql, rabbitMQ and Kafka message brokers, and monitoring via the micrometer tracing stack.

### Tech Stack

- Spring Boot 3.1.3+
- JDK 17+ (baseline for spring boot 3.0+)
- Spring Security (version 6.0+)
- Spring Discovery (based on the latest cloud version)
- Spring API Gateway
- Distributed tracing with zipkin and sleuth (all deprecated but replaces with micrometer tracing)
- Spring Kafka and Kafka streams for messaging

### Databases

- MongoDB
- MySQL and PostgreSQL

### Other Tech Stack

- Authorization via Spring OAuth2 resource server (using keycloak)
- Keycloack provides the authorization via OAuth2 & OIDC

### Others include

- Monitoring via Grafana and Prometheus
- Package and deploy using Docker, Kubernetes and Jenkins

### Steps to Build and Run the Application

This project standardizes container image builds with Dockerfiles. From the `spring-boot-app` directory, build and push all backend and frontend images with:

```bash
chmod +x build-and-push.sh
./build-and-push.sh
```

By default, images are pushed as `kaingyn615/<service>:main`. You can override the registry user and tag:

```bash
DOCKER_USERNAME=<docker-user> IMAGE_TAG=<tag> ./build-and-push.sh
```

To run locally with Docker Compose:

```bash
docker-compose up -d
```

For Kubernetes deployment, create the real Secret from the committed example before applying the Kustomize overlay:

```bash
cp k8s/secret-example.yaml k8s/secrets.yaml
# Update change-me values in k8s/secrets.yaml
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -k k8s
```

For staging, use the staging namespace and example secret:

```bash
cp k8s-staging/secret-example.yaml k8s-staging/secrets.yaml
# Update change-me values in k8s-staging/secrets.yaml
kubectl apply -f k8s-staging/namespace.yaml
kubectl apply -f k8s-staging/secrets.yaml
```

Next, run the following command to spin up keycloak and databases via docker-compose

`docker run --name keycloak_test -p 8181:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=password quay.io/keycloak/keycloak:18.0.0 start-dev`

Use any API platform (eg Postman, thunder client, Http etc) to perform CRUD operations on the endpoints. The naming of the individual services describe their main functions. For instance, The `product-service` represent the products in a given ecommerce application. Proceed with performing CRUD operations.

###  Other APIs 

- Grafana (visualization) and Prometheus (metrics), Tempo(traces), Loki (Logs) for monitoring and instrumentation
- Deployment using docker and kubernetes (still to be done)
- Updated services to use latest boot, security and cloud APIs

### Jenkins CI/CD

The repository root contains `Jenkinsfile` for the main CI/CD flow:

- Pull Request: run backend/frontend tests, SonarQube analysis, Quality Gate, Docker build and Trivy scan without pushing or deploying.
- `dev`: push images tagged `dev` and `dev-<short-sha>`, then deploy `k8s-staging`.
- `main`: push images tagged `main` and `main-<short-sha>`, then deploy `k8s`.

### Please Note
The Kubernetes manifests are managed through `k8s/kustomization.yaml` and `k8s-staging/kustomization.yaml`. Jenkins injects the commit image tag into a temporary Kustomize copy before deployment. Do not commit real secret files; keep only committed examples in Git.
