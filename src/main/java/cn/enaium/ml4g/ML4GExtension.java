package cn.enaium.ml4g;

import org.gradle.api.Project;

import java.io.File;

/**
 * @author Enaium
 */
public class ML4GExtension {
    public String gameVersion = "1.16.5";
    public String mainClass = "net.minecraft.client.main.Main";
    public String tweakClass = null;
    public String mixinRefMap = null;
    public String injectRemapping = null;

    private final Project project;

    public ML4GExtension(Project project) {
        this.project = project;
    }

    public File getUserCache() {
        File userCache = new File(project.getGradle().getGradleUserHomeDir(), "caches" + File.separator + "ml4g");

        if (!userCache.exists()) {
            userCache.mkdirs();
        }

        return userCache;
    }
}
