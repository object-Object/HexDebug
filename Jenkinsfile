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
            description: "Publish release to Maven (default: publish snapshot)",
            defaultValue: false,
        )
        booleanParam(
            name: "PUBLISH_CURSEFORGE",
            description: "Publish to CurseForge",
            defaultValue: false,
        )
        booleanParam(
            name: "PUBLISH_MODRINTH",
            description: "Publish to Modrinth",
            defaultValue: false,
        )
        booleanParam(
            name: "PUBLISH_GITHUB",
            description: "Publish to GitHub",
            defaultValue: false,
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
        stage("Publish: Maven") {
            steps {
                sh "./gradlew publish"
            }
        }
        stage("Publish: GitHub") {
            when {
                expression { return params.PUBLISH_GITHUB }
            }
            steps {
                sh "./gradlew publishGithub"
            }
        }
        stage("Publish: CurseForge") {
            when {
                expression { return params.PUBLISH_CURSEFORGE }
            }
            steps {
                sh "./gradlew publishCurseforge"
            }
        }
        stage("Publish: Modrinth") {
            when {
                expression { return params.PUBLISH_MODRINTH }
            }
            steps {
                sh "./gradlew publishModrinth"
            }
        }
    }
    post {
        always {
            archiveArtifacts "build/jenkinsArtifacts/*"
        }
    }
}
