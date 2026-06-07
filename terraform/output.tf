output "alb_dns_name" {
  description = "Public DNS of the ALB — point your Ingress hosts and your DNS CNAME here"
  value       = aws_lb.k8s.dns_name
}

output "alb_zone_id" {
  description = "Hosted zone ID of the ALB (use when creating Route53 alias records)"
  value       = aws_lb.k8s.zone_id
}

output "kube_api_endpoint" {
  description = "Direct kube API endpoint (ALB can't proxy TCP/6443)"
  value       = "https://${aws_instance.master[0].public_ip}:6443"
}

output "master_public_ips" {
  description = "Public IPs of k3s master nodes"
  value       = aws_instance.master[*].public_ip
}

output "worker_public_ips" {
  description = "Public IPs of k3s worker nodes"
  value       = aws_instance.workers[*].public_ip
}

output "master_private_ip" {
  description = "Private IP of k3s master node"
  value       = aws_instance.master[0].private_ip
}

output "security_group_id" {
  description = "Security Group ID for k3s cluster nodes"
  value       = aws_security_group.k8s_sg.id
}

output "alb_security_group_id" {
  description = "Security Group ID for the ALB"
  value       = aws_security_group.alb.id
}

output "ssh_key_file" {
  description = "Path to the SSH private key (only set when Terraform generated the key)"
  value       = var.ssh_public_key == "" ? "${path.module}/k3s-key.pem" : "(use your own key)"
}

output "k3s_token" {
  description = "Pre-shared k3s cluster token"
  value       = random_password.k3s_token.result
  sensitive   = true
}

output "ssh_connect_cmds" {
  description = "SSH commands to connect to cluster nodes"
  value = merge(
    {
      master = "ssh -i ${var.ssh_public_key == "" ? "./k3s-key.pem" : "<your-key>"} ubuntu@${aws_instance.master[0].public_ip}"
    },
    {
      for idx, w in aws_instance.workers :
      "worker${idx + 1}" => "ssh -i ${var.ssh_public_key == "" ? "./k3s-key.pem" : "<your-key>"} ubuntu@${w.public_ip}"
    }
  )
}

output "next_steps" {
  description = "What to run after apply"
  value       = <<-EOT
    # 1. Wait ~90s for k3s to bootstrap, then:
    ./get-kubeconfig.sh
    export KUBECONFIG=$(pwd)/kubeconfig
    kubectl get nodes -o wide

    # 2. Any Ingress / Service reachable via ALB at:
    http://${aws_lb.k8s.dns_name}
    ${var.acm_certificate_arn != "" ? "https://${aws_lb.k8s.dns_name}" : "# (set var.acm_certificate_arn to enable HTTPS)"}

    # 3. kube API (direct, no LB) is at:
    https://${aws_instance.master[0].public_ip}:6443
  EOT
}
