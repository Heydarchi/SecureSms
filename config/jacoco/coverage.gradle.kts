val classExclusion = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/databinding/**/*.*",
    "**/generated/**/*.*",
    "**/com/aospinsight/securesms/ui/**/*.*"
)

tasks.register("testDebugUnitTestCoverage", JacocoReport::class) {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for the debug unit tests."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/testDebugUnitTestCoverage/html"))
    }

    sourceDirectories.setFrom(
        files("${project.projectDir}/src/main/java"),
        files("${project.projectDir}/src/main/kotlin")
    )
    
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug")) {
            exclude(classExclusion)
        },
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(classExclusion)
        }
    )
    
    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")).include("**/*.exec"),
        fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec")
    )
}
