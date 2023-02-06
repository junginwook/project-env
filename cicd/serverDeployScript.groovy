
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

                        sh '''
                        cd deploy
                        cat > deploy.sh <<- _EOF_
                        #!/bin/bash
                        BUILD_PATH=$(ls /home/ec2-user/deploy/*.jar)
                        JAR_NAME=$(basename $BUILD_PATH)
                        echo "> build 파일명: $JAR_NAME"
                        
                        echo "> build 파일 복사"
                        DEPLOY_PATH=/home/ec2-user/
                        cp $BUILD_PATH $DEPLOY_PATH
                        
                        echo "> springboot-deploy.jar 교체"
                        CP_JAR_PATH=$DEPLOY_PATH$JAR_NAME
                        APPLICATION_JAR_NAME=springboot-deploy.jar
                        APPLICATION_JAR=$DEPLOY_PATH$APPLICATION_JAR_NAME
                        
                        ln -Tfs $CP_JAR_PATH $APPLICATION_JAR
                        
                        echo "> 현재 실행중인 애플리케이션 pid 확인"
                        CURRENT_PID=$(pgrep -f $APPLICATION_JAR_NAME)
                        
                        if [ -z $CURRENT_PID ]
                        then
                          echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다."
                        else
                          echo "> kill -15 $CURRENT_PID"
                          kill -15 $CURRENT_PID
                          sleep 5
                        fi
                        
                        echo "> $APPLICATION_JAR 배포"
                        nohup java -jar $APPLICATION_JAR > /dev/null 2> /dev/null < /dev/null &
                        _EOF_'''.stripIndent()


                        sh """
                        cd deploy
                        cat>appspec.yml<<-EOF
                        version: 0.0
                        os: linux
                        files:
                          - source:  /
                            destination: /home/ec2-user/deploy
                        
                        permissions:
                          - object: /
                            pattern: "**"
                            owner: ec2-user
                            group: ec2-user
                        
                        hooks:
                          ApplicationStart:
                            - location: deploy.sh
                              timeout: 60
                              runas: root
                        """.stripIndent()

                        sh """
                        cd deploy
                        zip -r deploy *
                        """


                        withAWS(credentials: "AWS_CREDENTIAL") {
                            s3Upload(
                                    path: "${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip",
                                    file: "/var/lib/jenkins/workspace/${env.JOB_NAME}/deploy/deploy.zip",
                                    bucket: "inwook-beanstalk-deploy"
                            )
                        }
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
                    echo "Upload S3 stage failed"
                }
                success {
                    echo "Upload S3 stage success"
                }
            }
        }
        stage("Deploy") {
            steps {
                script {
                    try {
                        withAWS(credentials: "AWS_CREDENTIAL") {
                            sh """
                                aws deploy create-deployment \
                                --application-name inwook-deploy \
                                --deployment-group-name inwook-deploy-group \
                                --region ap-northeast-2 \
                                --s3-location bucket=inwook-beanstalk-deploy,key=${env.JOB_NAME}/${env.BUILD_NUMBER}/${env.JOB_NAME}.zip,bundleType=zip \
                                --file-exists-behavior OVERWRITE \
                                --output json > DEPLOYMENT_ID.json
                                cat DEPLOYMENT_ID.json
                            """
                        }

                        def DEPLOYMENT_ID = readJSON file: './DEPLOYMENT_ID.json'
                        echo"${DEPLOYMENT_ID.deploymentId}"
                        sh("rm -rf ./DEPLOYMENT_ID.json")

                        awaitDeploymentCompletion("${DEPLOYMENT_ID.deploymentId}")
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
                    echo "Deploy stage failed"
                }
                success {
                    echo "Deploy stage success"
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