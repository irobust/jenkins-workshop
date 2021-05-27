# Pipeline Script
## Build Stage
Create pipeline project and add this script

### First job
```
node {
   echo 'Hello World'
}
```

### First Pipeline
```
pipeline {
    agent any
    stages {
        stage("Hello") {
                steps {
                    echo 'Hello World'
                }
        }
    }
}
```

### Working with Parameterize
```
pipeline {
    agent any
    parameters {
        string(name: 'Greeting', defaultValue: 'Hello', description: 'How should I greet the world?')
    }
    stages {
        stage('Example') {
            steps {
                echo "${params.Greeting} World!"
            }
        }
    }
}
```

### Multi Step
```
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'echo "Hello World"'
                sh '''
                    echo "Multiline shell steps works too"
                    ls -lah
                '''
            }
        }
    }
}
```

### Launch Shell Script with Timeout
1. Create pipeline project
2. docker exec -it jenkin-server bash
3. cd /var/jenkins-home
4. mkdir scripts
5. cat > fibonacci.sh
6. Exit with Ctl + C

```
pipeline {
    agent any
    stages {
        stage('Deploy') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    sh '/var/jenkins_home/scripts/fibonacci.sh 5'
                }
                timeout(time: 1, unit: 'MINUTES') {
                    sh '/var/jenkins_home/scripts/fibonacci.sh 32'
                }
            }
        }
    }
}
```

