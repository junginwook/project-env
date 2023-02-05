
pipeline {
    agent any

    tools {
        gradle "Gradle 7.6"
    }
    environment {
        GIT_DISTRIBUTE_URL = "https://github.com/junginwook/multi-module-projects.git"
    }
    stages {
        stage("Preparing Job") {
            steps {
                script {
                    try {
                        GIT_DISTRIBUTE_BRANCH_MAP = ["dev" : "develop", "qa" : "release", "prod" : "production"]

                        env.GIT_DISTRIBUTE_BRANCH = GIT_DISTRIBUTE_BRANCH_MAP[STAGE]

                        print("Deploy stage is ${STAGE}")
                        print("Deploy service is ${SERVICE}")
                        print("Deploy git branch is ${env.GIT_DISTRIBUTE_BRANCH}")
                    }
                    catch (error) {
                        print(error)
                        currentBuild.result = "FAILURE"
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
                        git url: GIT_DISTRIBUTE_URL, branch: GIT_DISTRIBUTE_BRANCH, credentialsId: "GIT_CREDENTIAL"
                    }
                    catch (error) {
                        print(error)
                        currentBuild.result = "FAILURE"
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
                        sh("rm -rf deploy")
                        sh("mkdir deploy")

                        sh("gradle clean ${SERVICE}:build -x test")

                        sh("cd deploy")
                        sh("cp /var/lib/jenkins/workspace/${env.JOB_NAME}/${SERVICE}/build/libs/*.jar ./deploy/${SERVICE}.jar")
                    }
                    catch (error) {
                        print(error)
                        sh("sudo rm -rf /var/lib/jenkins/workspace/${env.JOB_NAME}/*")
                        currentBuild.result = "FAILURE"
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
        stage("Upload To S3") {
            steps {
                script {
                    try {

                        sh("cd deploy")

                        sh("cat>deploy.sh <<-EOF " +
                                "\"#!/bin/bash\" " +
                                "BUILD_PATH=\$(ls /home/ec2-user/build/*.jar)\n " +
                                "JAR_NAME=\$(basename $BUILD_PATH)\n " +
                                "EOF")

//                        withAWS(credentials: "AWS_CREDENTIAL") {
//                            s3Upload(path: "aaa/bbb/ccc.zip")
//                        }
                    }
                    catch (error) {
                        print(error)
                        currentBuild.result = "FAILURE"
                    }
                }
            }
        }
        stage("Clean Up") {
            steps {
                script {
                    try {

                    }
                    catch (error) {
                        print(error)
                        currentBuild.result = "FAILURE"
                    }
                }
            }
        }
    }
}