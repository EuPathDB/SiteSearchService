plugins {
  java
  id("org.veupathdb.lib.gradle.container.container-utils") version "4.8.9"
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

containerBuild {

  project {
    name = "site-search-service"
    group = "org.veupathdb.service"
    version = "1.0.0"
    projectPackage = "org.gusdb.sitesearch.service"
    mainClassName = "Main"
  }

  docker {
    imageName = "veupathdb/site-search"
  }
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    name = "GitHubPackages"
    url  = uri("https://maven.pkg.github.com/veupathdb/packages")
    credentials {
      username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
      password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
    }
  }
}

java {
  targetCompatibility = JavaVersion.VERSION_17
  sourceCompatibility = JavaVersion.VERSION_17
}

tasks.shadowJar {
  exclude("**/Log4j2Plugins.dat")
  archiveFileName.set("service.jar")
}

configurations.all {
  resolutionStrategy {
    cacheChangingModulesFor(0, TimeUnit.SECONDS)
    cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
  }
}

dependencies {
  implementation(platform("org.gusdb:base-pom:2.16"))

  implementation("org.gusdb:fgputil-core:2.9.3")
  implementation("org.gusdb:fgputil-solr:2.9.3")
  implementation("org.gusdb:fgputil-json:2.9.3")
  implementation("org.gusdb:fgputil-web:2.9.3")
  implementation("org.gusdb:fgputil-server:2.9.3")

  implementation("org.veupathdb.lib:jaxrs-container-core:6.15.3")
  implementation("org.json:json")
  implementation("org.glassfish.jersey.core:jersey-client")
  implementation("org.apache.logging.log4j:log4j-core")

}
