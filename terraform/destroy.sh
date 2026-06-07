#!/bin/bash
# Tear down the k3s lab cluster on AWS.
# Run from the terraform/ directory: ./destroy.sh
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Resources to be destroyed:"
terraform state list 2>/dev/null || {
  echo "No terraform state found — nothing to destroy."
  exit 0
}

echo
read -rp "Confirm destroy ALL resources above? (yes/no): " ans
[[ "$ans" == "yes" ]] || { echo "Aborted."; exit 1; }

terraform destroy -auto-approve

echo "==> Cleaning local artifacts"
rm -f kubeconfig
unset KUBECONFIG 2>/dev/null || true

echo "==> Done. EC2 instances, security group, and key pair removed."
echo "    Local SSH key ~/.ssh/k3s_aws kept (delete manually if no longer needed)."
