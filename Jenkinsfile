pipeline {
    agent any

    triggers {
        pollSCM('* * * * *')
    }

    stages {
        stage("Docker build") {
            steps {
                sh "docker build -t sabbath666/jenkins-slave ."
            }
        }

        stage("Docker login") {
            steps {
                sh 'docker login -u sabbath666 -p sai7tie0'
            }
        }

        stage("Docker push") {
            steps {
                sh "docker push sabbath666/jenkins-slave"
            }
        }
    }
}