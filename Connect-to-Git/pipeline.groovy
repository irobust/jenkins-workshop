pipeline {
    agent any

    triggers { pollSCM('H * * * *') }
    stages {
        
        stage('Checkout'){
            steps {
                git url: 'https://github.com/irobust/jenkins-git', branch: 'master'
            }
        }
        
        stage('Hello') {
            steps {
                echo 'Hello World'
                sh 'false'
            }

            post {
                success {
                    sh 'echo build succees'
                }
                failure {
                    sh 'echo build fail'
                }
                always {
                    emailext attachLog: true, 
                             compressLog: true,
                             recipientProviders: [upstreamDevelopers(), requestor()], 
                             to: 'test@jenkins',
                             subject: 'Job \'${JOB_NAME}\' (${BUILD_NUMBER}) is ${BUILD_STATUS}',
                             body: 'Please go to ${BUILD_URL} and verify the build'
                }
            }
        }
    }
}