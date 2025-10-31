pipeline {
    agent any

    environment {
        // Git repository configuration
        GIT_REPO = 'https://github.com/RobertoDure/hr-rag-assistant.git' // Update with your repo URL
        GIT_BRANCH = 'main'

        // Docker configuration
        DOCKER_REGISTRY = 'https://hub.docker.com/repository/docker/robertodure/'
        BACKEND_IMAGE = "${DOCKER_REGISTRY}/hr-ragwiser-backend"
        FRONTEND_IMAGE = "${DOCKER_REGISTRY}/hr-ragwiser-frontend"
        POSTGRES_IMAGE = "pgvector/pgvector:pg16"
        IMAGE_TAG = "${BUILD_NUMBER}"

        // Kubernetes configuration
        K8S_NAMESPACE = 'hr-ragwiser'
        K8S_DEPLOYMENT_PATH = 'k8s'

        // Maven configuration
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    triggers {
        // Poll SCM every minute for changes
        pollSCM('* * * * *')
        // Alternative: Use webhook trigger
        // githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "=== Checking out code from Git ==="
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${GIT_BRANCH}"]],
                        userRemoteConfigs: [[url: "${GIT_REPO}"]]
                    ])
                }
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    echo "=== Building Backend with Maven ==="
                    if (isUnix()) {
                        sh 'mvn clean package -DskipTests'
                    } else {
                        bat 'mvn clean package -DskipTests'
                    }
                }
            }
        }

        stage('Test Backend') {
            steps {
                script {
                    echo "=== Running Backend Tests ==="
                    if (isUnix()) {
                        sh 'mvn test'
                    } else {
                        bat 'mvn test'
                    }
                }
            }
        }

        stage('Build Docker Images') {
            parallel {
                stage('Build Backend Image') {
                    steps {
                        script {
                            echo "=== Building Backend Docker Image ==="
                            docker.build("${BACKEND_IMAGE}:${IMAGE_TAG}", "-f Dockerfile .")
                            docker.build("${BACKEND_IMAGE}:latest", "-f Dockerfile .")
                        }
                    }
                }

                stage('Build Frontend Image') {
                    steps {
                        script {
                            echo "=== Building Frontend Docker Image ==="
                            docker.build("${FRONTEND_IMAGE}:${IMAGE_TAG}", "-f frontend/Dockerfile ./frontend")
                            docker.build("${FRONTEND_IMAGE}:latest", "-f frontend/Dockerfile ./frontend")
                        }
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    echo "=== Pushing Docker Images to Local Registry ==="
                    docker.withRegistry("${DOCKER_REGISTRY}") {
                        docker.image("${BACKEND_IMAGE}:${IMAGE_TAG}").push()
                        docker.image("${BACKEND_IMAGE}:latest").push()
                        docker.image("${FRONTEND_IMAGE}:latest").push()
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "=== Deploying to Local Kubernetes Cluster ==="

                    // Create namespace if it doesn't exist
                    if (isUnix()) {
                        sh """
                            kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        """
                    } else {
                        bat """
                            kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        """
                    }

                    // Apply Kubernetes configurations
                    if (isUnix()) {
                        sh """
                            # Apply ConfigMaps and Secrets
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/configmap.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/secret.yaml -n ${K8S_NAMESPACE}

                            # Apply PersistentVolumeClaim for PostgreSQL
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-pvc.yaml -n ${K8S_NAMESPACE}

                            # Deploy PostgreSQL
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-service.yaml -n ${K8S_NAMESPACE}

                            # Wait for PostgreSQL to be ready
                            kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s -n ${K8S_NAMESPACE}

                            # Deploy Backend
                            kubectl set image deployment/backend backend=${BACKEND_IMAGE}:${IMAGE_TAG} -n ${K8S_NAMESPACE} || \
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/backend-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/backend-service.yaml -n ${K8S_NAMESPACE}

                            # Deploy Frontend
                            kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE}:${IMAGE_TAG} -n ${K8S_NAMESPACE} || \
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/frontend-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/frontend-service.yaml -n ${K8S_NAMESPACE}

                            # Apply Ingress (optional)
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/ingress.yaml -n ${K8S_NAMESPACE}
                        """
                    } else {
                        bat """
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/configmap.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/secret.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-pvc.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/postgres-service.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/backend-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/backend-service.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/frontend-deployment.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/frontend-service.yaml -n ${K8S_NAMESPACE}
                            kubectl apply -f ${K8S_DEPLOYMENT_PATH}/ingress.yaml -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {
            agent any
            steps {
                script {
                    echo "=== Verifying Deployment Status ==="
                    if (isUnix()) {
                        sh """
                            echo "Waiting for deployments to be ready..."
                            kubectl rollout status deployment/postgres -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/frontend -n ${K8S_NAMESPACE} --timeout=300s

                            echo "\\n=== Deployment Status ==="
                            kubectl get deployments -n ${K8S_NAMESPACE}

                            echo "\\n=== Pod Status ==="
                            kubectl get pods -n ${K8S_NAMESPACE}

                            echo "\\n=== Service Status ==="
                            kubectl get services -n ${K8S_NAMESPACE}
                        """
                    } else {
                        bat """
                            kubectl rollout status deployment/postgres -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/backend -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/frontend -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl get deployments -n ${K8S_NAMESPACE}
                            kubectl get pods -n ${K8S_NAMESPACE}
                            kubectl get services -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "=== Pipeline completed successfully! ==="
            echo "Application deployed to namespace: ${K8S_NAMESPACE}"
            echo "Backend Image: ${BACKEND_IMAGE}:${IMAGE_TAG}"
            echo "Frontend Image: ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            echo """
            Access the application:
            - Frontend: http://localhost:30000 (NodePort) or via Ingress
            - Backend API: http://localhost:30080 (NodePort) or via Ingress
            """
        }
        failure {
            echo "=== Pipeline failed! Check logs for details ==="
        }
        always {
            // Clean up workspace if needed
            cleanWs(deleteDirs: true, patterns: [[pattern: 'target/**', type: 'INCLUDE']])
        }
    }
}

