terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

############################################
# Networking — reuse default VPC + subnets #
############################################

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

locals {
  subnet_ids       = data.aws_subnets.default.ids
  master_subnet_id = local.subnet_ids[0]
  # ALB needs >= 2 subnets in different AZs
  alb_subnet_ids = slice(local.subnet_ids, 0, min(length(local.subnet_ids), 2))
}

############################################
# Ubuntu 22.04 AMI (Canonical official)    #
############################################

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

############################################
# SSH key                                  #
############################################

resource "tls_private_key" "ssh" {
  count     = var.ssh_public_key == "" ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "this" {
  key_name   = "${var.project}-key"
  public_key = var.ssh_public_key != "" ? var.ssh_public_key : tls_private_key.ssh[0].public_key_openssh
}

resource "local_sensitive_file" "private_key" {
  count           = var.ssh_public_key == "" ? 1 : 0
  content         = tls_private_key.ssh[0].private_key_pem
  filename        = "${path.module}/k3s-key.pem"
  file_permission = "0600"
}

locals {
  ssh_key_path = var.ssh_public_key == "" ? "${path.module}/k3s-key.pem" : "<your-private-key>"
}

############################################
# k3s shared cluster token                 #
############################################

resource "random_password" "k3s_token" {
  length  = 48
  special = false
}

############################################
# Security groups                          #
############################################

resource "aws_security_group" "alb" {
  name        = "${var.project}-alb-sg"
  description = "ALB ingress from internet"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-alb-sg" }
}

resource "aws_security_group" "k8s_sg" {
  name        = "${var.project}-sg"
  description = "k3s cluster nodes"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "ICMP (ping)"
    from_port   = -1
    to_port     = -1
    protocol    = "icmp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Kubernetes API (direct master access - ALB cannot proxy this)"
    from_port   = 6443
    to_port     = 6443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description     = "HTTP from ALB"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description     = "HTTPS from ALB"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "NodePort range (direct access - handy for debugging)"
    from_port   = 30000
    to_port     = 32767
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "All internal cluster traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-sg" }
}

############################################
# Application Load Balancer                #
############################################

resource "aws_lb" "k8s" {
  name               = "${var.project}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = local.alb_subnet_ids

  tags = { Name = "${var.project}-alb" }
}

# Target group for plain HTTP traffic — backed by k3s Traefik on port 80.
# Traefik returns 404 for unknown hosts; accept that as healthy.
resource "aws_lb_target_group" "http" {
  name        = "${var.project}-tg-http"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "instance"

  health_check {
    protocol            = "HTTP"
    port                = "80"
    path                = "/"
    matcher             = "200-499"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.k8s.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.http.arn
  }
}

# HTTPS listener — only when an ACM cert is provided
resource "aws_lb_listener" "https" {
  count             = var.acm_certificate_arn != "" ? 1 : 0
  load_balancer_arn = aws_lb.k8s.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.http.arn
  }
}

############################################
# EC2 instances                            #
############################################

resource "aws_instance" "master" {
  count = 1

  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.master_instance_type
  key_name                    = aws_key_pair.this.key_name
  subnet_id                   = local.master_subnet_id
  vpc_security_group_ids      = [aws_security_group.k8s_sg.id]
  associate_public_ip_address = true

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  user_data = <<-EOT
    #!/bin/bash
    set -euxo pipefail
    sleep 10

    TOKEN=$(curl -sX PUT "http://169.254.169.254/latest/api/token" \
      -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    PUBLIC_IP=$(curl -sH "X-aws-ec2-metadata-token: $TOKEN" \
      http://169.254.169.254/latest/meta-data/public-ipv4)
    PRIVATE_IP=$(curl -sH "X-aws-ec2-metadata-token: $TOKEN" \
      http://169.254.169.254/latest/meta-data/local-ipv4)

    curl -sfL https://get.k3s.io | \
      INSTALL_K3S_VERSION="${var.k3s_version}" \
      K3S_TOKEN="${random_password.k3s_token.result}" \
      sh -s - server \
        --write-kubeconfig-mode=644 \
        --tls-san="$PUBLIC_IP" \
        --tls-san="$PRIVATE_IP" \
        --bind-address=0.0.0.0 \
        --node-ip="$PRIVATE_IP" \
        --advertise-address="$PRIVATE_IP"

    sleep 15
    chmod 644 /etc/rancher/k3s/k3s.yaml
  EOT

  root_block_device {
    volume_size = var.master_volume_size_gb
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project}-master-${count.index + 1}"
    Role = "k3s-master"
  }
}

resource "aws_instance" "workers" {
  count = var.worker_count

  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.worker_instance_type
  key_name                    = aws_key_pair.this.key_name
  subnet_id                   = local.subnet_ids[count.index % length(local.subnet_ids)]
  vpc_security_group_ids      = [aws_security_group.k8s_sg.id]
  associate_public_ip_address = true
  depends_on                  = [aws_instance.master]

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  user_data = <<-EOT
    #!/bin/bash
    set -euxo pipefail
    sleep 10

    TOKEN=$(curl -sX PUT "http://169.254.169.254/latest/api/token" \
      -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    PRIVATE_IP=$(curl -sH "X-aws-ec2-metadata-token: $TOKEN" \
      http://169.254.169.254/latest/meta-data/local-ipv4)

    MASTER_IP="${aws_instance.master[0].private_ip}"
    echo "Waiting for k3s server at $MASTER_IP..."
    until curl -ks "https://$MASTER_IP:6443/" >/dev/null 2>&1; do
      sleep 5
    done

    curl -sfL https://get.k3s.io | \
      INSTALL_K3S_VERSION="${var.k3s_version}" \
      K3S_URL="https://$MASTER_IP:6443" \
      K3S_TOKEN="${random_password.k3s_token.result}" \
      sh -s - agent --node-ip="$PRIVATE_IP"
  EOT

  root_block_device {
    volume_size = var.worker_volume_size_gb
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project}-worker-${count.index + 1}"
    Role = "k3s-worker"
  }
}

############################################
# Attach all nodes to the HTTP target group#
############################################

resource "aws_lb_target_group_attachment" "http" {
  count            = 1 + var.worker_count
  target_group_arn = aws_lb_target_group.http.arn
  target_id        = count.index == 0 ? aws_instance.master[0].id : aws_instance.workers[count.index - 1].id
  port             = 80
}

############################################
# Generated files (inventory + kubecfg)    #
############################################

resource "local_file" "kubespray_inventory" {
  content = templatefile("${path.module}/templates/inventory.tmpl", {
    master_ips         = aws_instance.master[*].public_ip
    master_private_ips = aws_instance.master[*].private_ip
    worker_ips         = aws_instance.workers[*].public_ip
    worker_private_ips = aws_instance.workers[*].private_ip
  })
  filename = "${path.module}/../inventory/mycluster/hosts.yaml"
}

resource "local_file" "get_kubeconfig" {
  content         = <<-EOT
    #!/bin/bash
    # Fetch kubeconfig from the k3s master.
    # ALB is L7 and cannot proxy the kube API, so the kubeconfig points
    # directly at the master's public IP.
    set -euo pipefail

    MASTER_IP="$${1:-${aws_instance.master[0].public_ip}}"
    KEY="$${SSH_KEY:-${local.ssh_key_path}}"

    echo "Fetching kubeconfig from $MASTER_IP ..."
    ssh -o StrictHostKeyChecking=no -i "$KEY" ubuntu@"$MASTER_IP" \
      'sudo cat /etc/rancher/k3s/k3s.yaml' \
      | sed "s|https://127.0.0.1:6443|https://$MASTER_IP:6443|" > ./kubeconfig
    chmod 600 ./kubeconfig

    echo "Done. Run:"
    echo "  export KUBECONFIG=$(pwd)/kubeconfig"
    echo "  kubectl get nodes"
  EOT
  filename        = "${path.module}/get-kubeconfig.sh"
  file_permission = "0755"
}

output "cluster_info" {
  description = "k3s cluster node information"
  sensitive   = true
  value = {
    vpc_id          = data.aws_vpc.default.id
    security_group  = aws_security_group.k8s_sg.id
    master_ip       = aws_instance.master[0].public_ip
    master_private  = aws_instance.master[0].private_ip
    worker_ips      = aws_instance.workers[*].public_ip
    worker_private  = aws_instance.workers[*].private_ip
    k3s_token       = random_password.k3s_token.result
  }
}

output "ssh_commands" {
  description = "SSH commands to connect to nodes"
  value = {
    master  = "ssh -i ~/.ssh/labsuser.pem ubuntu@${aws_instance.master[0].public_ip}"
    worker1 = "ssh -i ~/.ssh/labsuser.pem ubuntu@${aws_instance.workers[0].public_ip}"
    worker2 = length(aws_instance.workers) > 1 ? "ssh -i ~/.ssh/labsuser.pem ubuntu@${aws_instance.workers[1].public_ip}" : "no worker-2"
  }
}