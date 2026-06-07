#!/usr/bin/env bash
# Demo Argo Rollouts canary for product-service without changing the image.
# It triggers a new ReplicaSet by changing a pod-template annotation, then
# prints rollout/ReplicaSet state until the rollout becomes Healthy.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KUBECONFIG="${KUBECONFIG:-$REPO_ROOT/terraform/kubeconfig}"
NS="${NS:-spring-microservices}"
ROLLOUT="${ROLLOUT:-product-service}"
HPA="${HPA:-product-service-hpa}"
PIN_HPA="${PIN_HPA:-false}"

export KUBECONFIG

log() {
  printf "\n==> %s\n" "$*"
}

have_hpa() {
  kubectl -n "$NS" get hpa "$HPA" >/dev/null 2>&1
}

cleanup() {
  if [ -n "${HPA_BACKUP:-}" ] && [ -f "$HPA_BACKUP" ]; then
    log "Restoring HPA $HPA"
    kubectl apply -f "$HPA_BACKUP" >/dev/null || true
    rm -f "$HPA_BACKUP"
  fi
}

trap cleanup EXIT

log "Using kubeconfig: $KUBECONFIG"
kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'; echo

log "Preflight"
kubectl get nodes
kubectl -n "$NS" get rollout "$ROLLOUT"

if [ "$PIN_HPA" = "true" ] && have_hpa; then
  HPA_BACKUP="$(mktemp)"
  kubectl -n "$NS" get hpa "$HPA" -o yaml > "$HPA_BACKUP"
  log "Pinning HPA $HPA to 2 replicas during canary demo"
  kubectl -n "$NS" patch hpa "$HPA" --type merge \
    -p '{"spec":{"minReplicas":2,"maxReplicas":2}}' >/dev/null
fi

log "Before"
kubectl -n "$NS" get rs -l app="$ROLLOUT"
kubectl -n "$NS" get pods -l app="$ROLLOUT" -o wide

RUN_ID="$(date +%s)"
log "Triggering canary rollout with demo-run=$RUN_ID"
kubectl -n "$NS" patch rollout "$ROLLOUT" --type merge \
  -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"demo-run\":\"$RUN_ID\"}}}}}"

log "Watching ReplicaSets until rollout is Healthy"
printf "Tip: use 'kubectl argo rollouts get rollout %s -n %s --watch' if the Argo Rollouts plugin is installed.\n" "$ROLLOUT" "$NS"

for _ in $(seq 1 80); do
  printf "\n--- %s ---\n" "$(date '+%H:%M:%S')"
  kubectl -n "$NS" get rs -l app="$ROLLOUT"
  kubectl -n "$NS" get rollout "$ROLLOUT"

  phase="$(kubectl -n "$NS" get rollout "$ROLLOUT" -o jsonpath='{.status.phase}' 2>/dev/null || true)"
  ready="$(kubectl -n "$NS" get rollout "$ROLLOUT" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || true)"
  updated="$(kubectl -n "$NS" get rollout "$ROLLOUT" -o jsonpath='{.status.updatedReplicas}' 2>/dev/null || true)"

  if [ "$phase" = "Healthy" ] && [ "${ready:-0}" = "2" ] && [ "${updated:-0}" = "2" ]; then
    break
  fi

  sleep 10
done

log "Final rollout details"
kubectl -n "$NS" describe rollout "$ROLLOUT" | sed -n '/Status:/,/Events:/p'

log "Final ReplicaSets"
kubectl -n "$NS" get rs -l app="$ROLLOUT"

log "Done"
