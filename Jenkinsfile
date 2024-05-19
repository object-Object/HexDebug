#!/usr/bin/env groovy
// noinspection GroovyAssignabilityCheck

pipeline {
    agent any
    tools {
        jdk "jdk-17"
    }
    parameters {
        booleanParam(
            name: "PUBLISH_MAVEN_RELEASE",
            description: "Publish release to Maven",
            defaultValue: false
        )
        booleanParam(
            name: "PUBLISH_CURSEFORGE",
            description: "Publish to CurseForge",
            defaultValue: false
        )
        booleanParam(
            name: "PUBLISH_MODRINTH",
            description: "Publish to Modrinth",
            defaultValue: false
        )
        booleanParam(
            name: "PUBLISH_GITHUB",
            description: "Publish to GitHub",
            defaultValue: false
        )
    }
    stages {
        stage("Clean") {
            steps {
                sh "chmod +x gradlew"
                sh "./gradlew clean"
            }
        }
        stage("Build") {
            steps {
                sh "./gradlew build"
            }
        }
        stage("Publish") {
            failFast false
            parallel {
                stage("Maven Release") {
                    when {
                        expression { return params.PUBLISH_MAVEN_RELEASE }
                    }
                    steps {
                        echo "Publishing release to Maven"
                    }
                }
                stage("Maven Snapshot") {
                    when {
                        expression { return !params.PUBLISH_MAVEN_RELEASE }
                    }
                    steps {
                        echo "Publishing snapshot to Maven"
                    }
                }
                stage("CurseForge") {
                    when {
                        expression { return params.PUBLISH_CURSEFORGE }
                    }
                    steps {
                        echo "Publishing to CurseForge"
                    }
                }
                stage("Modrinth") {
                    when {
                        expression { return params.PUBLISH_MODRINTH }
                    }
                    steps {
                        echo "Publishing to Modrinth"
                    }
                }
                stage("GitHub") {
                    when {
                        expression { return params.PUBLISH_GITHUB }
                    }
                    steps {
                        echo "Publishing to GitHub"
                    }
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts "build/jenkinsArtifacts/*"
        }
    }
}
