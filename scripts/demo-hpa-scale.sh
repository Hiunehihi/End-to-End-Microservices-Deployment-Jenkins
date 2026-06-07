#!/usr/bin/env bash
# Demo HorizontalPodAutoscaler for product-service.
# Keep this script running while recording Grafana; press Ctrl+C to stop load
# and restore the HPA settings.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KUBECONFIG="${KUBECONFIG:-$REPO_ROOT/terraform/kubeconfig}"
NS="${NS:-spring-microservices}"
ROLLOUT="${ROLLOUT:-product-service}"
SERVICE="${SERVICE:-product-service}"
HPA="${HPA:-product-service-hpa}"
LOAD_PREFIX="${LOAD_PREFIX:-load-product}"
LOAD_PODS="${LOAD_PODS:-8}"
CPU_TARGET="${CPU_TARGET:-5}"
MIN_REPLICAS="${MIN_REPLICAS:-2}"
MAX_REPLICAS="${MAX_REPLICAS:-6}"
TARGET_URL="${TARGET_URL:-http://product-service:8080/actuator/health}"

export KUBECONFIG
CLEANED_UP=false

log() {
  printf "\n==> %s\n" "$*"
}

cleanup() {
  if [ "$CLEANED_UP" = "true" ]; then
    return
  fi
  CLEANED_UP=true

  log "Stopping load pods"
  kubectl -n "$NS" delete pod -l "hpa-demo=$LOAD_PREFIX" --ignore-not-found >/dev/null || true

  if [ -n "${HPA_BACKUP:-}" ] && [ -f "$HPA_BACKUP" ]; then
    log "Restoring original HPA"
    kubectl -n "$NS" patch hpa "$HPA" --type merge -p "{\"spec\":$(cat "$HPA_BACKUP")}" >/dev/null || true
    rm -f "$HPA_BACKUP"
  else
    log "Leaving HPA at min=$MIN_REPLICAS max=$MAX_REPLICAS target=${CPU_TARGET}%"
  fi
}

on_signal() {
  cleanup
  exit 130
}

trap cleanup EXIT
trap on_signal INT TERM

log "Using kubeconfig: $KUBECONFIG"
kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'; echo

log "Preflight"
kubectl get nodes
kubectl -n "$NS" get rollout "$ROLLOUT"
kubectl -n "$NS" get svc "$SERVICE"

if kubectl -n "$NS" get hpa "$HPA" >/dev/null 2>&1; then
  HPA_BACKUP="$(mktemp)"
  kubectl -n "$NS" get hpa "$HPA" -o jsonpath='{.spec}' > "$HPA_BACKUP"
else
  HPA_BACKUP=""
fi

log "Configuring HPA $HPA for demo"
cat <<EOF | kubectl apply -f -
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: $HPA
  namespace: $NS
spec:
  scaleTargetRef:
    apiVersion: argoproj.io/v1alpha1
    kind: Rollout
    name: $ROLLOUT
  minReplicas: $MIN_REPLICAS
  maxReplicas: $MAX_REPLICAS
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: $CPU_TARGET
EOF

log "Current HPA"
kubectl -n "$NS" get hpa "$HPA"

log "Cleaning old load pods"
kubectl -n "$NS" delete pod -l "hpa-demo=$LOAD_PREFIX" --ignore-not-found >/dev/null || true
for i in $(seq 1 "$LOAD_PODS"); do
  kubectl -n "$NS" delete pod "$LOAD_PREFIX-$i" --ignore-not-found >/dev/null || true
done

log "Starting $LOAD_PODS load pods against $TARGET_URL"
for i in $(seq 1 "$LOAD_PODS"); do
  kubectl -n "$NS" run "$LOAD_PREFIX-$i" \
    --image=busybox:1.36 \
    --restart=Never \
    --labels="hpa-demo=$LOAD_PREFIX" \
    -- /bin/sh -c "while true; do wget -q -O- '$TARGET_URL' >/dev/null; done" >/dev/null
done

cat <<EOF

==================================================================
Grafana demo checklist
==================================================================
1. Open Grafana and filter namespace: $NS
2. Watch workload/pods for: $ROLLOUT
3. You should see CPU rise first, then pod count increase.
4. Keep this script running while recording.
5. Press Ctrl+C here when the video is done; load pods will be deleted.
==================================================================
EOF

log "Watching HPA, product pods, and CPU every 15s"
while true; do
  printf "\n--- %s ---\n" "$(date '+%H:%M:%S')"
  kubectl -n "$NS" get hpa "$HPA"
  kubectl -n "$NS" get pods -l app="$ROLLOUT" --no-headers | awk '{print $1, $2, $3, $5}'
  kubectl -n "$NS" top pods 2>/dev/null | awk -v rollout="$ROLLOUT" -v load="$LOAD_PREFIX" '$1 ~ rollout || $1 ~ load {print $1, $2, $3}' || true
  sleep 15
done
