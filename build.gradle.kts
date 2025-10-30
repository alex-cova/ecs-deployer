plugins {
    id("application")
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.example"
version = "1.0-SNAPSHOT"

extra["awsVersion"] = "2.37.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awssdk:ecs")
    implementation("software.amazon.awssdk:ecr")
    compileOnly("org.jetbrains:annotations:26.0.2")

    implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:${property("awsVersion")}")
    }
}


tasks.test {
    useJUnitPlatform()
}
