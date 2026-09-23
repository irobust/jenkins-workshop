# Hands-on Lab: Build and Push Docker Images with Jenkins to Sonatype Nexus

## Overview

This hands-on lab demonstrates how to build a Docker container image using a **Jenkins Pipeline** and push the image to a **Sonatype Nexus Repository 3 Docker Hosted Repository**.

By completing this lab, you will understand how to build a basic CI pipeline that performs:

```text
Git Repository
      |
      v
   Jenkins
      |
      | docker build
      v
 Docker Image
      |
      | docker login
      | docker push
      v
Sonatype Nexus
 Docker Registry
```

## Learning Objectives

After completing this lab, you will be able to:

* Deploy Jenkins using Docker Compose
* Deploy Sonatype Nexus Repository 3
* Configure Nexus as a Docker container registry
* Configure Jenkins to access Docker
* Store Nexus credentials securely in Jenkins
* Create a Jenkins Pipeline
* Build Docker images from a Jenkins Pipeline
* Tag Docker images
* Push Docker images to Nexus
* Verify images in Nexus
* Pull and run images from Nexus
* Use Jenkins build numbers as Docker image tags
* Use Git commit SHA as part of an image tag

---

# 1. Lab Architecture

The lab uses the following architecture:

```text
                         Git Repository
                              |
                              | checkout
                              v
                     +----------------+
                     |    Jenkins     |
                     |                |
                     | Jenkinsfile    |
                     +-------+--------+
                             |
                             | docker build
                             v
                     +----------------+
                     | Docker Engine  |
                     +-------+--------+
                             |
                             | docker push
                             v
                  +-----------------------+
                  |   Sonatype Nexus      |
                  |                       |
                  | Docker Hosted Repo    |
                  |                       |
                  | Port 8082             |
                  +-----------------------+
```

The lab environment consists of:

| Component     |  Port | Purpose               |
| ------------- | ----: | --------------------- |
| Jenkins       |  8080 | CI/CD server          |
| Jenkins Agent | 50000 | Jenkins inbound agent |
| Nexus         |  8081 | Nexus Web UI          |
| Nexus         |  8082 | Docker Registry       |

---

# 2. Prerequisites

Before starting this lab, install:

* Docker
* Docker Compose
* Git
* A web browser

Verify Docker:

```bash
docker version
```

Verify Docker Compose:

```bash
docker compose version
```

Verify Git:

```bash
git --version
```

You should have at least:

```text
Docker
Docker Compose
Git
```

---

# 3. Create Lab Directory

Create the lab directory:

```bash
mkdir jenkins-nexus-lab
cd jenkins-nexus-lab
```

Create the following structure:

```text
jenkins-nexus-lab/
├── docker-compose.yml
├── jenkins/
│   └── Dockerfile
├── app/
│   ├── Dockerfile
│   ├── package.json
│   └── server.js
└── Jenkinsfile
```

---

# 4. Create Docker Compose Environment

Create:

```text
docker-compose.yml
```

Add:

```yaml
services:

  nexus:
    image: sonatype/nexus3
    container_name: nexus
    restart: unless-stopped

    ports:
      - "8081:8081"
      - "8082:8082"

    volumes:
      - nexus-data:/nexus-data

    networks:
      - cicd

  jenkins:
    build:
      context: ./jenkins

    container_name: jenkins
    restart: unless-stopped

    ports:
      - "8080:8080"
      - "50000:50000"

    volumes:
      - jenkins-data:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock

    networks:
      - cicd

volumes:

  nexus-data:
  jenkins-data:

networks:

  cicd:
    driver: bridge
```

The important part for this lab is:

```yaml
- /var/run/docker.sock:/var/run/docker.sock
```

This allows Jenkins to communicate with the Docker Engine running on the Docker host.

> This Docker socket approach is convenient for a training lab. It gives Jenkins significant control over the host Docker daemon, so use a more isolated build architecture for production environments.

---

# 5. Create Jenkins Dockerfile

Create:

```text
jenkins/Dockerfile
```

Add:

```dockerfile
FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update \
    && apt-get install -y docker.io \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
```

This installs the Docker CLI inside the Jenkins container.

---

# 6. Start the Environment

Build and start the containers:

```bash
docker compose up -d --build
```

Check the containers:

```bash
docker compose ps
```

Expected result:

```text
NAME       STATUS       PORTS
jenkins    Up           0.0.0.0:8080->8080/tcp
nexus      Up           0.0.0.0:8081->8081/tcp
```

Check logs:

```bash
docker compose logs -f
```

Press:

```text
Ctrl+C
```

to stop following the logs.

---

# 7. Configure Nexus

Open Nexus:

```text
http://localhost:8081
```

Wait for Nexus to finish starting.

Check the Nexus container:

```bash
docker logs nexus
```

---

# 8. Get the Nexus Admin Password

Run:

```bash
docker exec nexus cat /nexus-data/admin.password
```

Example:

```text
a4d8c7e6-xxxx-xxxx-xxxx-xxxxxxxx
```

Save this password temporarily.

---

# 9. Login to Nexus

Open:

```text
http://localhost:8081
```

Click:

```text
Sign in
```

Use:

```text
Username: admin
Password: <password-from-admin.password>
```

Follow the initial setup wizard.

For this lab, you can keep the default configuration.

---

# 10. Create Docker Hosted Repository

Navigate to:

```text
Settings
    |
    +-- Repositories
```

Click:

```text
Create repository
```

Select:

```text
docker (hosted)
```

Configure:

```text
Name:
docker-hosted

HTTP:
8082
```

For this lab, leave the other settings at their defaults unless your environment requires otherwise.

Click:

```text
Create repository
```

Your Nexus Docker registry is now:

```text
localhost:8082
```

---

# 11. Verify Nexus Docker Registry

From the Docker host, try:

```bash
docker login localhost:8082
```

Enter your Nexus credentials.

If Nexus is configured to use HTTP, Docker may return:

```text
http: server gave HTTP response to HTTPS client
```

This happens because Docker expects registries to use HTTPS by default.

For a local training lab, configure Docker to allow the Nexus HTTP registry.

---

# 12. Configure Docker Insecure Registry

Edit:

```text
/etc/docker/daemon.json
```

Add:

```json
{
  "insecure-registries": [
    "localhost:8082"
  ]
}
```

If Docker is running on another machine, use the Nexus host address:

```json
{
  "insecure-registries": [
    "192.168.1.100:8082"
  ]
}
```

Restart Docker:

```bash
sudo systemctl restart docker
```

Verify:

```bash
docker info
```

Look for:

```text
Insecure Registries:
    localhost:8082
```

> For production environments, use HTTPS instead of an insecure HTTP registry.

---

# 13. Test Docker Push Manually

Before involving Jenkins, verify that Nexus works as a Docker registry.

Pull an image:

```bash
docker pull nginx:alpine
```

Tag it:

```bash
docker tag nginx:alpine localhost:8082/test/nginx:1.0
```

Login:

```bash
docker login localhost:8082
```

Push:

```bash
docker push localhost:8082/test/nginx:1.0
```

Expected output:

```text
The push refers to repository [localhost:8082/test/nginx]
...
1.0: digest: sha256:...
```

Go to:

```text
Nexus
    |
    +-- Repositories
        |
        +-- docker-hosted
```

You should see the Docker image.

---

# 14. Create Sample Application

Now create a simple application that Jenkins will build.

Create:

```text
app/server.js
```

Add:

```javascript
const http = require("http");

const port = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
    res.writeHead(200, {
        "Content-Type": "text/plain"
    });

    res.end("Hello from Jenkins + Nexus!\n");
});

server.listen(port, () => {
    console.log(`Server running on port ${port}`);
});
```

---

# 15. Create package.json

Create:

```text
app/package.json
```

Add:

```json
{
  "name": "jenkins-nexus-demo",
  "version": "1.0.0",
  "description": "Jenkins and Nexus Docker CI/CD demo",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  }
}
```

---

# 16. Create Application Dockerfile

Create:

```text
app/Dockerfile
```

Add:

```dockerfile
FROM node:22-alpine

WORKDIR /app

COPY package.json .

COPY server.js .

EXPOSE 3000

CMD ["npm", "start"]
```

Test the image manually:

```bash
cd app
```

Build:

```bash
docker build -t demo-app:local .
```

Run:

```bash
docker run --rm -p 3000:3000 demo-app:local
```

Open:

```text
http://localhost:3000
```

You should see:

```text
Hello from Jenkins + Nexus!
```

Stop the container with:

```text
Ctrl+C
```

Return to the project directory:

```bash
cd ..
```

---

# 17. Configure Jenkins

Open:

```text
http://localhost:8080
```

Jenkins will display the initial administrator password.

Get it with:

```bash
docker exec jenkins \
  cat /var/jenkins_home/secrets/initialAdminPassword
```

Copy the password into the Jenkins setup screen.

Select:

```text
Install suggested plugins
```

Wait for installation to complete.

Create the Jenkins administrator account.

---

# 18. Verify Docker Access from Jenkins

Run:

```bash
docker exec jenkins docker version
```

You should see both:

```text
Client:
 ...

Server:
 ...
```

This confirms that Jenkins can communicate with Docker.

You can also test:

```bash
docker exec jenkins docker ps
```

The command should display containers running on the Docker host.

---

# 19. Configure Jenkins Credentials

In Jenkins, go to:

```text
Manage Jenkins
    |
    +-- Credentials
        |
        +-- System
            |
            +-- Global credentials
```

Click:

```text
Add Credentials
```

Select:

```text
Kind:
Username with password
```

Configure:

```text
Username: admin
Password: <Nexus password>

ID:
nexus-docker

Description:
Nexus Docker Registry Credentials
```

Click:

```text
Create
```

The Jenkins Pipeline will reference this credential using:

```text
nexus-docker
```

---

# 20. Create Jenkinsfile

Create:

```text
Jenkinsfile
```

Add:

```groovy
pipeline {

    agent any

    environment {
        NEXUS_URL = "nexus:8082"
        IMAGE_NAME = "demo-app"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                      -t ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG} \
                      ./app
                '''
            }
        }

        stage('Login to Nexus') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-docker',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {

                    sh '''
                        echo "$NEXUS_PASSWORD" | \
                        docker login ${NEXUS_URL} \
                        --username "$NEXUS_USER" \
                        --password-stdin
                    '''
                }
            }
        }

        stage('Push Image') {
            steps {
                sh '''
                    docker push \
                      ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

    }

    post {

        always {
            sh '''
                docker logout ${NEXUS_URL} || true
            '''
        }

    }
}
```

---

# 21. Important: Nexus Hostname

The Jenkins container and Nexus container are connected through the Docker network:

```text
cicd
```

Therefore Jenkins can access Nexus using:

```text
nexus:8082
```

rather than:

```text
localhost:8082
```

This is important.

Inside the Jenkins container:

```text
localhost
```

means:

```text
Jenkins container
```

not the Docker host and not the Nexus container.

Therefore use:

```text
nexus:8082
```

inside the Jenkins Pipeline.

---

# 22. Create a Git Repository

Initialize Git:

```bash
git init
```

Add files:

```bash
git add .
```

Commit:

```bash
git commit -m "Initial Jenkins Nexus lab"
```

Your repository should contain:

```text
jenkins-nexus-lab/
├── docker-compose.yml
├── Jenkinsfile
├── jenkins/
│   └── Dockerfile
└── app/
    ├── Dockerfile
    ├── package.json
    └── server.js
```

Push the project to your Git server.

For example:

```bash
git remote add origin <YOUR_GIT_REPOSITORY>
git branch -M main
git push -u origin main
```

---

# 23. Create Jenkins Pipeline

In Jenkins:

```text
New Item
```

Enter:

```text
Name:
jenkins-nexus-demo
```

Select:

```text
Pipeline
```

Click:

```text
OK
```

Scroll to:

```text
Pipeline
```

Select:

```text
Definition:
Pipeline script from SCM
```

Select:

```text
SCM:
Git
```

Enter your Git repository URL:

```text
https://github.com/<username>/jenkins-nexus-lab.git
```

Select:

```text
Branch:
*/main
```

Set:

```text
Script Path:
Jenkinsfile
```

Click:

```text
Save
```

---

# 24. Run the Jenkins Pipeline

Click:

```text
Build Now
```

Jenkins should execute:

```text
Checkout
   |
   v
Build Docker Image
   |
   v
Login to Nexus
   |
   v
Push Image
```

A successful build should show:

```text
Finished: SUCCESS
```

---

# 25. Verify Docker Image

Inside the Docker host, list images:

```bash
docker images
```

You should see something similar to:

```text
REPOSITORY          TAG
nexus:8082/demo-app 1
```

The tag corresponds to:

```groovy
BUILD_NUMBER
```

For example:

```text
Build #1
    |
    +-- demo-app:1

Build #2
    |
    +-- demo-app:2
```

---

# 26. Verify Image in Nexus

Open:

```text
http://localhost:8081
```

Navigate to:

```text
Repositories
    |
    +-- docker-hosted
```

You should see:

```text
demo-app
```

with versions such as:

```text
1
2
3
```

---

# 27. Pull the Image from Nexus

Logout from Nexus:

```bash
docker logout localhost:8082
```

Login again:

```bash
docker login localhost:8082
```

Pull the image:

```bash
docker pull localhost:8082/demo-app:1
```

Run it:

```bash
docker run --rm \
  -p 3000:3000 \
  localhost:8082/demo-app:1
```

Open:

```text
http://localhost:3000
```

Expected:

```text
Hello from Jenkins + Nexus!
```

---

# 28. Improve Image Tagging

Using only:

```text
BUILD_NUMBER
```

works for the lab, but in a real CI/CD pipeline it is useful to include the Git commit SHA.

Modify the Jenkinsfile.

Add:

```groovy
stage('Prepare') {

    steps {

        script {

            env.GIT_COMMIT_SHORT = sh(
                script: "git rev-parse --short HEAD",
                returnStdout: true
            ).trim()

            env.IMAGE_TAG =
                "${BUILD_NUMBER}-${GIT_COMMIT_SHORT}"
        }
    }
}
```

The resulting image could look like:

```text
demo-app:15-a82f31c
```

This allows you to identify:

```text
Jenkins Build = 15
Git Commit    = a82f31c
```

---

# 29. Improved Jenkinsfile

A more complete version is:

```groovy
pipeline {

    agent any

    environment {
        NEXUS_URL = "nexus:8082"
        IMAGE_NAME = "demo-app"
    }

    stages {

        stage('Checkout') {

            steps {
                checkout scm
            }
        }

        stage('Prepare') {

            steps {

                script {

                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG =
                        "${BUILD_NUMBER}-${GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Build Docker Image') {

            steps {

                sh '''
                    docker build \
                      -t ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG} \
                      ./app
                '''
            }
        }

        stage('Login to Nexus') {

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-docker',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {

                    sh '''
                        echo "$NEXUS_PASSWORD" | \
                        docker login ${NEXUS_URL} \
                        --username "$NEXUS_USER" \
                        --password-stdin
                    '''
                }
            }
        }

        stage('Push Image') {

            steps {

                sh '''
                    docker push \
                      ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

    }

    post {

        always {

            sh '''
                docker logout ${NEXUS_URL} || true
            '''
        }
    }
}
```

---

# 30. Exercise: Add `latest` Tag

Modify the pipeline so that each successful build creates:

```text
demo-app:<BUILD_NUMBER>
```

and:

```text
demo-app:latest
```

For example:

```text
demo-app:15
demo-app:latest
```

Hint:

```bash
docker tag
```

and:

```bash
docker push
```

---

# 31. Exercise: Add Docker Image Testing

Add a stage:

```text
Test Docker Image
```

The stage should:

1. Start the Docker container
2. Send an HTTP request
3. Verify HTTP status code `200`
4. Stop the container

Example concept:

```bash
docker run -d \
  --name demo-test \
  -p 3000:3000 \
  ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG}
```

Then:

```bash
curl http://localhost:3000
```

Finally:

```bash
docker rm -f demo-test
```

---

# 32. Exercise: Add Trivy Security Scanning

Extend the pipeline:

```text
Checkout
   |
   v
Build
   |
   v
Security Scan
   |
   v
Login
   |
   v
Push
```

Use Trivy to scan the image:

```bash
trivy image ${NEXUS_URL}/${IMAGE_NAME}:${IMAGE_TAG}
```

The objective is to prevent images with unacceptable vulnerabilities from being pushed to Nexus.

---

# 33. Exercise: Add Version Tags

Instead of only using Jenkins build numbers, create a version such as:

```text
1.0.0
```

Push:

```text
demo-app:1.0.0
```

Then create:

```text
demo-app:latest
```

The final Nexus repository should contain:

```text
demo-app
├── 1
├── 2
├── 3
├── 1.0.0
└── latest
```

---

# 34. Troubleshooting

## Problem 1: `docker: command not found`

Check:

```bash
docker exec jenkins docker version
```

If Docker CLI is missing, rebuild Jenkins:

```bash
docker compose build jenkins
docker compose up -d
```

---

## Problem 2: Docker permission denied

Check:

```bash
docker exec jenkins docker ps
```

If you receive a permission error, check the Docker socket:

```bash
ls -l /var/run/docker.sock
```

The Jenkins container must be able to access the Docker socket.

---

## Problem 3: `connection refused`

Check Nexus:

```bash
docker ps
```

Check:

```bash
docker logs nexus
```

Verify Nexus is listening:

```bash
curl http://localhost:8081
```

---

## Problem 4: Docker cannot connect to Nexus

Inside Jenkins, test:

```bash
docker exec jenkins \
  curl http://nexus:8081
```

If this works, Docker networking is working.

Check the Docker registry port:

```text
nexus:8082
```

---

## Problem 5: HTTP/HTTPS error

If you see:

```text
http: server gave HTTP response to HTTPS client
```

you are probably using an HTTP Nexus Docker registry.

For the lab, configure:

```json
{
  "insecure-registries": [
    "localhost:8082"
  ]
}
```

For production, use HTTPS.

---

## Problem 6: `401 Unauthorized`

Check the Jenkins credential:

```text
nexus-docker
```

Verify:

```text
Username
Password
```

Also verify that the Nexus user has permission to push to the Docker hosted repository.

---

# 35. Final CI/CD Workflow

At the end of this lab, you should have:

```text
                 Developer
                     |
                     | git push
                     v
               Git Repository
                     |
                     | webhook / build
                     v
              +-------------+
              |   Jenkins   |
              +------+------+
                     |
                     | checkout
                     v
                Source Code
                     |
                     | docker build
                     v
                Docker Image
                     |
                     | security scan
                     v
                Trivy Scan
                     |
                     | docker login
                     v
                Nexus Registry
                     |
                     | docker push
                     v
              +--------------+
              | Nexus Docker |
              |    Hosted    |
              +------+-------+
                     |
                     | docker pull
                     v
              Application Server
```

---

# 36. Key Concepts

## Jenkins

Jenkins is responsible for:

```text
Source Checkout
       |
       v
Build
       |
       v
Test
       |
       v
Security Scan
       |
       v
Package
       |
       v
Publish
```

## Docker

Docker packages the application into an immutable container image.

Example:

```text
demo-app:15-a82f31c
```

## Nexus

Nexus provides a private container registry:

```text
Nexus
 |
 +-- docker-hosted
      |
      +-- demo-app
           |
           +-- 15-a82f31c
           +-- 16-b91a21e
```

## Jenkins Credentials

Credentials should be stored in Jenkins:

```text
Jenkins Credentials
        |
        v
nexus-docker
        |
        +-- Username
        +-- Password
```

Do not hard-code passwords inside:

```text
Jenkinsfile
Dockerfile
Git repository
```

---

# 37. Production Considerations

The configuration in this lab is designed for learning.

For production environments, consider:

### Use HTTPS

Instead of:

```text
http://nexus.example.com:8082
```

use:

```text
https://nexus.example.com
```

### Use Dedicated Jenkins Agents

Instead of building directly on the Jenkins controller, use dedicated agents:

```text
Jenkins Controller
        |
        v
Jenkins Agent
        |
        v
Docker Build
```

### Use Immutable Image Tags

Prefer:

```text
1.2.3
```

or:

```text
125-a82f31c
```

instead of relying only on:

```text
latest
```

### Add Security Scanning

Example:

```text
Trivy
```

### Add Image Signing

Consider:

```text
Cosign
```

### Add Deployment

The next stage can be:

```text
Jenkins
   |
   v
Nexus
   |
   v
Kubernetes
```

---

# 38. Lab Challenge

Build a complete pipeline:

```text
Checkout
   |
   v
Unit Test
   |
   v
Docker Build
   |
   v
Trivy Scan
   |
   v
Docker Login
   |
   v
Push to Nexus
   |
   v
Deploy
```

The final image should use:

```text
<registry>/<application>:<build>-<git-sha>
```

For example:

```text
nexus:8082/demo-app:25-a82f31c
```

The pipeline should fail if the security scan detects vulnerabilities above the configured severity threshold.

---

# 39. Expected Result

At the end of the workshop:

```text
Git
 |
 | Push
 v
Jenkins
 |
 | Docker Build
 v
Docker Image
 |
 | Security Scan
 v
Trivy
 |
 | Docker Push
 v
Nexus
 |
 +-- docker-hosted
      |
      +-- demo-app
           |
           +-- 1-a82f31c
           +-- 2-b91c22e
           +-- 3-f721ac9
```

You have now created a basic **Container CI Pipeline** using:

```text
Git
 +
Jenkins
 +
Docker
 +
Sonatype Nexus
 +
Trivy
```

This provides the foundation for the next lab:

```text
Jenkins + Nexus + Kubernetes
```

where Jenkins builds and pushes the container image to Nexus and then deploys that image to a Kubernetes cluster.
