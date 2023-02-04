
pipeline {
    agent any

    tools {
        gradle "Gradle 8.0-rc-2"
    }
    environment {

    }
    stages {
        stage("Preparing Job") {

        }
        stage("Cloning Git") {

        }
        stage("Building Jar") {

        }
        stage("Building Image") {

        }
    }
}