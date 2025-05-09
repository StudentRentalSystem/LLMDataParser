plugins {
    id("java")
    id("maven-publish")
}

group = "com.github.studentrental"
version = "1.0.3"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.json:json:20231013")
    implementation("io.milvus:milvus-sdk-java:2.5.7")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("org.apache.lucene:lucene-core:8.9.0")
    implementation("org.apache.lucene:lucene-analyzers-common:8.9.0")
    implementation("org.apache.lucene:lucene-queryparser:8.9.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {

    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "com.github.studentRentalSystem"
            artifactId = "LLMDataParser"
            version = "1.0.3"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/StudentRentalSystem/LLMDataParser")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
