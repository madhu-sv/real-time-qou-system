plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
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

}

tasks.withType<Test> {
	useJUnitPlatform()
}
