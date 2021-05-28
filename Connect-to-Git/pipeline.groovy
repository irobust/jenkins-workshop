pipeline {
    agent any

    stages {
        
        stage('Checkout'){
            steps {
                git url: 'https://github.com/irobust/jenkins-git', branch: 'master'
            }
        }
        
        stage('Hello') {
            steps {
                echo 'Hello World'
            }
        }
    }
}