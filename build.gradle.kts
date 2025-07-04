plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
	// Add the OWASP plugin
	id("org.owasp.dependencycheck") version "9.2.0"
}

group = "com.madhu"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starter
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	
	// Lombok for constructors, etc. (still useful in Java)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Elasticsearch Client
    implementation("co.elastic.clients:elasticsearch-java")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("jakarta.json:jakarta.json-api:2.1.3") // Updated version

    // Apache Commons Text for normalization
    implementation("org.apache.commons:commons-text:1.12.0")

	// OpenCSV for reading Kaggle data
	implementation("com.opencsv:opencsv:5.9")
	// Swagger / OpenAPI documentation
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

	testImplementation("org.testcontainers:junit-jupiter:1.19.8")
	testImplementation("org.testcontainers:elasticsearch:1.19.8")

}


tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register("generatePatterns") {
    group = "build"
    description = "Regenerate patterns.json from build_patterns.py"
    inputs.files("build_patterns.py", "requirements.txt")
    outputs.file("patterns.json")
    doLast {
        project.exec {
            commandLine("python3", "-m", "pip", "install", "-r", "requirements.txt")
        }
        project.exec {
            commandLine("python3", "build_patterns.py")
        }
    }
}

tasks.named("processResources") {
	dependsOn("generatePatterns")
}
