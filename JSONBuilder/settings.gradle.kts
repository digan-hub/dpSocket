rootProject.name = "JSONBuilder"
include("src")
for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "build.gradle"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
    }
}