pipeline {
  agent { label 'built-in' }

  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    string(name: 'DOCKER_NAMESPACE', defaultValue: 'kaingyn615', description: 'Docker Hub namespace used for all service images.')
    string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'dockerhub', description: 'Jenkins credential ID for Docker Hub username/password.')
    booleanParam(name: 'DEPLOY_ENABLED', defaultValue: true, description: 'Deploy only for dev and main branch builds.')
    string(name: 'STAGING_API_BASE_URL', defaultValue: 'http://localhost:31085', description: 'React API base URL for local k3d staging images.')
    string(name: 'PRODUCTION_API_BASE_URL', defaultValue: 'http://localhost:30085', description: 'React API base URL for local k3d production images.')
  }

  environment {
    APP_DIR = 'spring-boot-app'
    HOME = "${WORKSPACE}/.home"
    DOCKER_HOST = 'tcp://localhost:2375'
    DOCKER_CONFIG = "${WORKSPACE}/.docker"
    MAVEN_OPTS = "-Dmaven.repo.local=${WORKSPACE}/.m2/repository"
    NPM_CONFIG_CACHE = "${WORKSPACE}/.npm"
    SONAR_USER_HOME = "${WORKSPACE}/.sonar"
    XDG_CACHE_HOME = "${WORKSPACE}/.cache"
    SONARQUBE_SERVER = 'sonarqube'
    SONAR_SCANNER_TOOL = 'sonar-scanner'
    TRIVY_SEVERITY = 'CRITICAL,HIGH'
    JAVA_SERVICES = 'product-service order-service inventory-service notification-service api-gateway discovery-server admin-server cart-service payment-service'
    ALL_SERVICES = 'product-service order-service inventory-service notification-service api-gateway discovery-server admin-server cart-service payment-service frontend'
  }

  stages {
    stage('Init') {
      steps {
        sh 'mkdir -p "$HOME" "$DOCKER_CONFIG" "$WORKSPACE/.m2/repository" "$NPM_CONFIG_CACHE" "$SONAR_USER_HOME" "$XDG_CACHE_HOME"'
        script {
          env.SHORT_SHA = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
          env.ACTUAL_BRANCH = env.CHANGE_BRANCH ?: env.BRANCH_NAME ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
          env.IS_PR = env.CHANGE_ID ? 'true' : 'false'

          if (env.IS_PR == 'true') {
            env.DEPLOY_ENV = 'none'
            env.K8S_OVERLAY = ''
            env.K8S_NAMESPACE = ''
            env.BRANCH_TAG = "pr-${env.CHANGE_ID}"
            env.IMAGE_TAG = "${env.BRANCH_TAG}-${env.SHORT_SHA}"
            env.REACT_APP_API_BASE_URL = params.STAGING_API_BASE_URL
          } else if (env.ACTUAL_BRANCH == 'dev') {
            env.DEPLOY_ENV = 'staging'
            env.K8S_OVERLAY = "${env.APP_DIR}/k8s-staging"
            env.K8S_NAMESPACE = 'spring-microservices-staging'
            env.BRANCH_TAG = 'dev'
            env.IMAGE_TAG = "dev-${env.SHORT_SHA}"
            env.REACT_APP_API_BASE_URL = params.STAGING_API_BASE_URL
          } else if (env.ACTUAL_BRANCH == 'main') {
            env.DEPLOY_ENV = 'production'
            env.K8S_OVERLAY = "${env.APP_DIR}/k8s"
            env.K8S_NAMESPACE = 'spring-microservices'
            env.BRANCH_TAG = 'main'
            env.IMAGE_TAG = "main-${env.SHORT_SHA}"
            env.REACT_APP_API_BASE_URL = params.PRODUCTION_API_BASE_URL
          } else {
            env.DEPLOY_ENV = 'none'
            env.K8S_OVERLAY = ''
            env.K8S_NAMESPACE = ''
            env.BRANCH_TAG = env.ACTUAL_BRANCH.replaceAll('[^A-Za-z0-9_.-]', '-')
            env.IMAGE_TAG = "${env.BRANCH_TAG}-${env.SHORT_SHA}"
            env.REACT_APP_API_BASE_URL = params.STAGING_API_BASE_URL
          }

          currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.ACTUAL_BRANCH}@${env.SHORT_SHA}"
          echo "Branch=${env.ACTUAL_BRANCH}, PR=${env.IS_PR}, deployEnv=${env.DEPLOY_ENV}, imageTag=${env.IMAGE_TAG}"
        }
      }
    }

    stage('Backend Test') {
      steps {
        dir("${env.APP_DIR}") {
          sh '''
            set -eu
            mvn -B clean verify
            for service in $JAVA_SERVICES; do
              mvn -B -pl "$service" spring-boot:repackage
            done
          '''
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: "${env.APP_DIR}/**/target/surefire-reports/*.xml"
          archiveArtifacts allowEmptyArchive: true, artifacts: "${env.APP_DIR}/**/target/site/jacoco/**"
        }
      }
    }

    stage('Backend SonarQube') {
      steps {
        dir("${env.APP_DIR}") {
          withSonarQubeEnv("${env.SONARQUBE_SERVER}") {
            sh '''
              mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar \
                -Dsonar.projectKey=spring-boot-app-backend \
                -Dsonar.projectName=spring-boot-app-backend \
                -Dsonar.userHome="$SONAR_USER_HOME"
            '''
          }
        }
      }
    }

    stage('Backend Quality Gate') {
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Frontend Test') {
      steps {
        dir("${env.APP_DIR}/frontend") {
          sh 'npm ci'
          sh 'npm test -- --watchAll=false --coverage'
          sh 'npm run build'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: "${env.APP_DIR}/frontend/junit.xml"
          archiveArtifacts allowEmptyArchive: true, artifacts: "${env.APP_DIR}/frontend/coverage/**"
        }
      }
    }

    stage('Frontend SonarQube') {
      steps {
        dir("${env.APP_DIR}/frontend") {
          script {
            def scannerHome = tool "${env.SONAR_SCANNER_TOOL}"
            withSonarQubeEnv("${env.SONARQUBE_SERVER}") {
              sh "${scannerHome}/bin/sonar-scanner"
            }
          }
        }
      }
    }

    stage('Frontend Quality Gate') {
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Build Images') {
      steps {
        dir("${env.APP_DIR}") {
          sh '''
            set -eu
            for service in $JAVA_SERVICES; do
              docker build \
                -f "$service/Dockerfile" \
                -t "$DOCKER_NAMESPACE/$service:$IMAGE_TAG" \
                -t "$DOCKER_NAMESPACE/$service:$BRANCH_TAG" \
                .
            done

            docker build \
              --build-arg REACT_APP_API_BASE_URL="$REACT_APP_API_BASE_URL" \
              -t "$DOCKER_NAMESPACE/frontend:$IMAGE_TAG" \
              -t "$DOCKER_NAMESPACE/frontend:$BRANCH_TAG" \
              frontend
          '''
        }
      }
    }

    stage('Trivy Scan') {
      steps {
        sh '''
          set -eu
          for service in $ALL_SERVICES; do
            trivy image \
              --exit-code 1 \
              --ignore-unfixed \
              --vuln-type os,library \
              --severity "$TRIVY_SEVERITY" \
              "$DOCKER_NAMESPACE/$service:$IMAGE_TAG"
          done
        '''
      }
    }

    stage('Push Images') {
      when {
        expression { return env.IS_PR != 'true' && (env.ACTUAL_BRANCH == 'dev' || env.ACTUAL_BRANCH == 'main') }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: params.DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
          sh '''
            set -eu
            mkdir -p "$DOCKER_CONFIG"
            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
            for service in $ALL_SERVICES; do
              docker push "$DOCKER_NAMESPACE/$service:$IMAGE_TAG"
              docker push "$DOCKER_NAMESPACE/$service:$BRANCH_TAG"
            done
          '''
        }
      }
    }

    stage('Deploy Staging') {
      when {
        expression { return params.DEPLOY_ENABLED && env.ACTUAL_BRANCH == 'dev' && env.IS_PR != 'true' }
      }
      steps {
        withCredentials([file(credentialsId: 'kubeconfig-staging', variable: 'KUBECONFIG_FILE')]) {
          sh '''
            set -eu
            export KUBECONFIG="$KUBECONFIG_FILE"
            WORK_DIR="$(mktemp -d)"
            cp -R "$K8S_OVERLAY/." "$WORK_DIR/"
            sed -i "s/newTag: $BRANCH_TAG/newTag: $IMAGE_TAG/g" "$WORK_DIR/kustomization.yaml"
            kubectl -n "$K8S_NAMESPACE" delete job staging-smoke-test --ignore-not-found=true
            kubectl apply -k "$WORK_DIR"
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/discovery-server --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/api-gateway --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/order-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/inventory-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/notification-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/admin-server --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/cart-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/payment-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/frontend --timeout=300s
            kubectl -n "$K8S_NAMESPACE" wait --for=condition=complete job/staging-smoke-test --timeout=300s
          '''
        }
      }
    }

    stage('Deploy Production') {
      when {
        expression { return params.DEPLOY_ENABLED && env.ACTUAL_BRANCH == 'main' && env.IS_PR != 'true' }
      }
      steps {
        withCredentials([file(credentialsId: 'kubeconfig-production', variable: 'KUBECONFIG_FILE')]) {
          sh '''
            set -eu
            export KUBECONFIG="$KUBECONFIG_FILE"
            WORK_DIR="$(mktemp -d)"
            cp -R "$K8S_OVERLAY/." "$WORK_DIR/"
            sed -i "s/newTag: $BRANCH_TAG/newTag: $IMAGE_TAG/g" "$WORK_DIR/kustomization.yaml"
            kubectl apply -k "$WORK_DIR"
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/discovery-server --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/api-gateway --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/order-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/inventory-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/notification-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/admin-server --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/cart-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/payment-service --timeout=300s
            kubectl -n "$K8S_NAMESPACE" rollout status deployment/frontend --timeout=300s
          '''
        }
      }
    }
  }

  post {
    always {
      sh 'docker logout || true'
      deleteDir()
    }
  }
}
