
version = "2.0"
plugins {
    alias(libs.plugins.paperweight)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin)
}
repositories{
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    //implementation("com.ezylang:EvalEx:3.2.0")
    //Crux Modules
    compileOnly(fileTree("libs") {
        include("*.jar")
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

allprojects{

    plugins.apply("java")

    repositories {
        mavenCentral()
        maven("https://redempt.dev")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<Test> {
        systemProperty("file.encoding", "UTF-8")
    }

    tasks.withType<Javadoc>{
        options.encoding = "UTF-8"
    }
}

tasks.runServer {
    minecraftVersion("26.2")
    pluginJars(files("libs/CruxCore.jar"))
}
