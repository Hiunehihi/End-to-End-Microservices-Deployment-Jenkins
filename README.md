# 🚀 End-to-End Microservices Deployment

Dự án này thực hiện tự động hóa hoàn toàn quy trình xây dựng hạ tầng Cloud, khởi tạo cụm Kubernetes và triển khai hệ thống Microservices thương mại điện tử chuyên nghiệp.

## 🛠 Công nghệ cốt lõi

- **Cloud Provider:** AWS (Amazon Web Services).
- **IaC:** Terraform (Quản lý EC2, VPC, Security Groups).
- **Configuration Management:** Ansible & Kubespray.
- **Orchestration:** Kubernetes v1.30+.
- **Backend:** Spring Boot 3, Spring Cloud, Kafka, MongoDB, PostgreSQL.
- **Frontend:** React (TypeScript), VietQR/VNPay Integration.
- **Security:** Keycloak (OAuth2/OIDC).
- **CI/CD:** Jenkins, SonarQube, Trivy, Docker Hub, Kubernetes/Kustomize.

---

## 🏗 Giai đoạn 1: Hạ tầng & Kubernetes

Hạ tầng được khởi tạo trên AWS EC2 bằng Terraform. File inventory cho cụm Kubernetes được sinh ra trong thư mục `inventory/mycluster`.

### 1. Khởi tạo hạ tầng với Terraform

```bash
cd terraform
terraform init
terraform apply -auto-approve
```

### 2. Chuẩn bị cụm Kubernetes

Repo đã có sẵn inventory cho cụm Kubernetes tại `inventory/mycluster/hosts.yaml`. Nếu dùng Kubespray, chạy playbook từ thư mục Kubespray riêng và trỏ về inventory này:

```bash
ansible-playbook -i ../inventory/mycluster/hosts.yaml cluster.yml
```

---

## 📦 Giai đoạn 2: Build & Push Images

Toàn bộ mã nguồn ứng dụng nằm trong thư mục `spring-boot-app`.

Hệ thống thống nhất build image bằng `Dockerfile` cho toàn bộ backend services và frontend. Bạn cần đăng nhập Docker Hub trước khi chạy script.

```bash
cd spring-boot-app
chmod +x build-and-push.sh

# Mặc định push lên kaingyn615/<service>:main
./build-and-push.sh

# Có thể override Docker Hub user hoặc tag nếu cần
DOCKER_USERNAME=<docker-user> IMAGE_TAG=<tag> ./build-and-push.sh
```

Tag mặc định là `main` để khớp với `spring-boot-app/k8s/kustomization.yaml`.

---

## ☸️ Giai đoạn 3: Deploy Kubernetes

Trước khi deploy, tạo Kubernetes Secret thật từ file mẫu. File `secrets.yaml` đã được `.gitignore` để tránh commit credential lên Git.

```bash
cp spring-boot-app/k8s/secret-example.yaml spring-boot-app/k8s/secrets.yaml

# Chỉnh các giá trị change-me trong secrets.yaml trước khi apply
kubectl apply -f spring-boot-app/k8s/namespace.yaml
kubectl apply -f spring-boot-app/k8s/secrets.yaml
```

Sau đó deploy toàn bộ manifest bằng Kustomize để Kubernetes dùng đúng image tag trong `spring-boot-app/k8s/kustomization.yaml`.

```bash
kubectl apply -k spring-boot-app/k8s
```

Kustomize sẽ apply các nhóm tài nguyên chính:

- `namespace.yaml`
- `databases.yaml`
- `kafka-zipkin.yaml`
- `keycloak.yaml`
- `canary-rollouts.yaml`
- `ecommerce-expansion.yaml`
- `services.yaml`
- `frontend.yaml`

### Production-readiness đã bổ sung

Các manifest Kubernetes đã được bổ sung những cấu hình giúp deployment gần với môi trường production hơn:

- `PersistentVolumeClaim` cho MongoDB, PostgreSQL và MySQL để dữ liệu không mất khi Pod restart.
- `readinessProbe` để Kubernetes chỉ route traffic đến Pod khi service đã sẵn sàng.
- `livenessProbe` để Kubernetes tự restart Pod khi service bị treo.
- `resources.requests` và `resources.limits` để kiểm soát CPU/RAM cho workload.
- `valueFrom.secretKeyRef` để không hard-code password/VNPay secret trực tiếp trong Deployment manifest.

---

## 🔁 Giai đoạn 4: CI/CD với Jenkins

Pipeline Jenkins đã được cấu hình trong file `Jenkinsfile` ở root repo. Nên tạo job dạng **Multibranch Pipeline** để Jenkins tự nhận nhánh `dev`, `main` và Pull Request.

Luồng triển khai:

- Pull Request: chạy build, unit test, SonarQube Quality Gate, build image local và scan Trivy; không push image và không deploy.
- Nhánh `dev`: chạy CI, push image tag `dev` và `dev-<short-sha>`, deploy lên staging bằng `spring-boot-app/k8s-staging`, sau đó chạy smoke test.
- Merge Pull Request vào `main`: chạy CI, push image tag `main` và `main-<short-sha>`, deploy lên production bằng `spring-boot-app/k8s`.

Jenkins agent cần có sẵn:

- JDK 17, Maven, Node.js 18, Docker CLI/daemon.
- `kubectl`.
- `trivy`.
- Jenkins plugins: Pipeline, Docker Pipeline hoặc Docker CLI access, SonarQube Scanner, JUnit.

Cấu hình Jenkins cần chuẩn bị:

- SonarQube server name: `sonarqube`.
- Sonar Scanner tool name: `sonar-scanner`.
- Username/password credential ID: `dockerhub-credentials`.
- File credential ID cho kubeconfig staging: `kubeconfig-staging`.
- File credential ID cho kubeconfig production: `kubeconfig-production`.

Secret thật vẫn cần được tạo trong cluster trước khi Jenkins deploy:

```bash
cp spring-boot-app/k8s/secret-example.yaml spring-boot-app/k8s/secrets.yaml
cp spring-boot-app/k8s-staging/secret-example.yaml spring-boot-app/k8s-staging/secrets.yaml
# Chỉnh các giá trị change-me trước khi apply

kubectl apply -f spring-boot-app/k8s/namespace.yaml
kubectl apply -f spring-boot-app/k8s/secrets.yaml

kubectl apply -f spring-boot-app/k8s-staging/namespace.yaml
kubectl apply -f spring-boot-app/k8s-staging/secrets.yaml
```

Khi build nhánh `dev` hoặc `main`, Jenkins tự build/test/scan/push image và deploy bằng `kubectl apply -k`. Image tag theo commit (`dev-<short-sha>` hoặc `main-<short-sha>`) được inject vào bản Kustomize tạm thời trong workspace, không cần commit thay đổi tag vào Git.

---

## 🧩 Các tính năng chính của ứng dụng

- **Storefront:** Giao diện mua sắm hiện đại, hiển thị tồn kho thời gian thực.
- **Cart Service:** Quản lý giỏ hàng đồng bộ với danh mục sản phẩm.
- **VNPay & VietQR:** Tích hợp thanh toán qua cổng VNPay Sandbox và tạo mã VietQR chuyển khoản nhanh.
- **Inventory Control:** Chặn đặt hàng nếu vượt quá số lượng tồn kho, tự động trừ kho sau khi thanh toán thành công qua Kafka.

---

## 🎯 Kết quả đạt được

- [x] Hạ tầng AWS tự động hóa hoàn toàn.
- [x] Cụm Kubernetes Production Ready.
- [x] CI/CD Jenkins cho staging và production.
- [x] Hệ thống Microservices giao tiếp thông suốt qua Kafka và Service Discovery.
- [x] Luồng thanh toán thương mại điện tử hoàn chỉnh với kiểm soát kho nghiêm ngặt.
