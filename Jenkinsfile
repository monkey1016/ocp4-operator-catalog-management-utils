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
            dir('docker-build') {
              sh 'mv ../target/operator-catalog-tools-*.jar ./'
              sh 'mv ../../Dockerfile ./'
            }
          }
        }

        script {
            dir('operator-utils-api-service/docker-build') {
            openshift.startBuild('operator-utils-api-service', "--from-dir=operator-utils-api-service/target")
          }
        }
      }
    }
  }
}