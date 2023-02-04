
pipeline {
    agent any

    tools {
        gradle "Gradle 8.0-rc-2"
    }
    environment {

    }
    stages {
        stage("Preparing Job") {
            steps {

            }
            post {

            }
        }
        stage("Cloning Git") {
            steps {
                
            }
        }
        stage("Building Jar") {
            steps {

            }
        }
        stage("Building Image") {
            steps {

            }
        }
    }
}