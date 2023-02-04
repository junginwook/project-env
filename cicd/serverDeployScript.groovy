
pipeline {
    agent any

    tools {
        gradle "Gradle 8.0-rc-2"
    }
    environment {
        APP = "module"

    }
    stages {
        stage("Preparing Job") {
            steps {
                script {
                    try {
                        GIT_DISTRIBUTE_BRANCH_MAP = ["dev" : "develop", "qa" : "release", "prod" : "production"]

                        env.SERVICE_NAME = "${APP}-${SERVICE}"
                        env.GIT_DISTRIBUTE_BRANCH = GIT_DISTRIBUTE_BRANCH_MAP[STAGE]

                        print("Deploy stage is ${STAGE}")
                        print("Deploy service is ${env.SERVICE_NAME}")
                        print("Deploy git branch is ${env.GIT_DISTRIBUTE_BRANCH}")
                    }
                    catch (error) {
                        print(error)
                    }
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