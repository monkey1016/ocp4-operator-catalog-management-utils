pipeline {
    agent {
        label 'maven'
    }

    stages {
        stage('Build') {
            steps {
                withMaven() {
                    dir('operator-utils-api-service') {
                        sh 'mvn clean install'
                    }
                }

                script {
                    openshift.startBuild('operator-utils-api-service', "--from-dir=operator-utils-api-service/target")
                }
            }
        }
    }
}