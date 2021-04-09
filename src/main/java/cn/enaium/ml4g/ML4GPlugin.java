package cn.enaium.ml4g;

import cn.enaium.ml4g.task.*;
import cn.enaium.ml4g.util.GameUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static cn.enaium.ml4g.util.UrlUtil.GAME_LIBRARIES;

/**
 * @author Enaium
 */
public class ML4GPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("=============================");
        project.getLogger().lifecycle("Minecraft Launcher for Gradle");
        project.getLogger().lifecycle("          By Enaium          ");
        project.getLogger().lifecycle("=============================");

        project.getExtensions().create("minecraft", ML4GExtension.class, project);
        ML4GExtension extension = project.getExtensions().getByType(ML4GExtension.class);

        //Add libraries
        project.getRepositories().maven(mavenArtifactRepository -> {
            mavenArtifactRepository.setName("minecraft");
            mavenArtifactRepository.setUrl(GAME_LIBRARIES);
        });

        project.getRepositories().maven(mavenArtifactRepository -> {
            mavenArtifactRepository.setName("Enaium");
            mavenArtifactRepository.setUrl("https://maven.enaium.cn/");
        });

        project.getRepositories().maven(mavenArtifactRepository -> {
            mavenArtifactRepository.setName("SpongePowered");
            mavenArtifactRepository.setUrl("https://repo.spongepowered.org/maven/");
        });


        project.getRepositories().mavenCentral();
        project.getRepositories().mavenLocal();

        GameUtil.getLibraries(extension.gameVersion).forEach(library -> project.getDependencies().add("implementation", library));

        project.getPlugins().apply("java");
        project.getPlugins().apply("idea");
        project.getTasks().create("downloadMapping", DownloadMappingTask.class, downloadMappingTask -> downloadMappingTask.setGroup("ml4g"));
        project.getTasks().create("downloadGame", DownloadGameTask.class, downloadGameTask -> downloadGameTask.setGroup("ml4g"));
        project.getTasks().create("cleanGame", CleanGameTask.class, cleanGameTask -> cleanGameTask.setGroup("ml4g"));
        project.getTasks().create("downloadAsset", DownloadAssetTask.class, downloadAssetTask -> downloadAssetTask.setGroup("ml4g"));
        project.getTasks().create("remappingClass", RemappingClassTask.class, remappingClassTask -> remappingClassTask.setGroup("ml4g"));
        project.getTasks().create("genIdeaRun", GenIdeaRunTask.class, genIdeaRunTask -> genIdeaRunTask.setGroup("ml4g"));

        project.getTasks().getByName("idea").finalizedBy(project.getTasks().getByName("downloadMapping"), project.getTasks().getByName("downloadGame"), project.getTasks().getByName("cleanGame"), project.getTasks().getByName("downloadAsset"), project.getTasks().getByName("genIdeaRun"));

        project.getTasks().getByName("compileJava").finalizedBy(project.getTasks().getByName("remappingClass"));

        project.afterEvaluate(project1 -> {
            project1.getDependencies().add("compileOnly", project1.getDependencies().create(project1.files(GameUtil.getClientCleanFile(extension).getAbsolutePath())));
            project1.getDependencies().add("runtimeOnly", project1.getDependencies().create(project1.files(GameUtil.getClientFile(extension).getAbsolutePath())));
        });
    }
}
