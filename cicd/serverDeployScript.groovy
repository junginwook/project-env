
pipeline {
    agent any

    tools {
        gradle "Gradle 8.0-rc-2"
    }
    stages {
        stage("Preparing Job") {
            steps {
                script {
                    print("Preparing Job")
                }
            }
        }
        stage("Cloning Git") {
            steps {
                script {
                    print("Cloning Git")
                }
            }
        }
        stage("Building Jar") {
            steps {
                script {
                    print("Building Jar")
                }
            }
        }
        stage("Building Image") {
            steps {
                script {
                    print("Building Image")
                }
            }
        }
    }
}