
pipeline {
    agent any

    tools {
        gradle "Gradle 7.6"
    }
    environment {
        APP = "module"
        GIT_DISTRIBUTE_URL = "https://github.com/junginwook/multi-module-projects.git"
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
            post {
                failure {
                    echo "Preparing Job stage failed"
                }
                success {
                    echo "Preparing Job stage success"
                }
            }
        }
        stage("Cloning Git") {
            steps {
                script {
                    try {
                        git url: GIT_DISTRIBUTE_URL, branch: GIT_DISTRIBUTE_BRANCH, credentialsId: "lucky"
                    }
                    catch (error) {
                        print(error)
                    }
                }
            }
            post {
                failure {
                    echo "Git clone stage failed"
                }
                success {
                    echo "Git clone stage success"
                }
            }
        }
        stage("Building Jar") {
            steps {
                script {
                    try {
                        sh("gradle clean ${env.SERVICE_NAME}:build -x test")
                    }
                    catch (error) {
                        print(error)
                    }
                }
            }
            post {
                failure {
                    echo "Build jar stage failed"
                }
                success {
                    echo "Build jar stage success"
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