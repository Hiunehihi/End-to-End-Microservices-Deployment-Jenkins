#!/bin/bash
set -euo pipefail

DOCKER_USERNAME="${DOCKER_USERNAME:-kaingyn615}"
IMAGE_TAG="${IMAGE_TAG:-main}"

echo "Bắt đầu build và push images bằng Dockerfile"
echo "Docker Hub user: ${DOCKER_USERNAME}"
echo "Image tag: ${IMAGE_TAG}"

JAVA_SERVICES=(
    "product-service"
    "order-service"
    "inventory-service"
    "notification-service"
    "api-gateway"
    "discovery-server"
    "admin-server"
    "cart-service"
    "payment-service"
)

for SERVICE in "${JAVA_SERVICES[@]}"; do
    IMAGE="${DOCKER_USERNAME}/${SERVICE}:${IMAGE_TAG}"

    echo "--------------------------------------------------------"
    echo "Build Java service: ${SERVICE}"
    echo "Image: ${IMAGE}"
    echo "--------------------------------------------------------"

    docker build -f "${SERVICE}/Dockerfile" -t "${IMAGE}" .
    docker push "${IMAGE}"
done

FRONTEND_IMAGE="${DOCKER_USERNAME}/frontend:${IMAGE_TAG}"

echo "--------------------------------------------------------"
echo "Build frontend"
echo "Image: ${FRONTEND_IMAGE}"
echo "--------------------------------------------------------"

docker build -t "${FRONTEND_IMAGE}" frontend
docker push "${FRONTEND_IMAGE}"

echo "Hoàn tất build và push tất cả images."
