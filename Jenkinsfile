pipeline {
  agent {
    label 'maven'
  }

  stages {
    stage('Build JAR') {
      steps {
        git branch: 'khaled-build', url: 'https://github.com/monkey1016/ocp4-operator-catalog-management-utils.git'
        dir('operator-utils-api-service') {
          sh 'mvn clean install'
          dir('docker-build') {
            sh 'mv ../target/operator-catalog-tools-*.jar ./'
            sh 'mv ../../Dockerfile ./'
          }
        }
      }
    }

    stage('Build Image') {
      steps {
        script {
          dir('operator-utils-api-service/docker-build') {
            openshift.withCluster() {
              openshift.startBuild('operator-utils-api-service', "--from-dir=.")
            }
          }
        }
      }
    }
  }
}