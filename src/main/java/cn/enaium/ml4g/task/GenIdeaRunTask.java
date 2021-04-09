package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import cn.enaium.ml4g.util.GameUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Enaium
 */
public class GenIdeaRunTask extends Task {
    @TaskAction
    public void genIdeaRunTask() {

        StringBuilder vmArgs = new StringBuilder("-Djava.library.path=" + GameUtil.getNativeFileDir(extension).getAbsolutePath());
        StringBuilder programArgs = new StringBuilder();

        if (extension.tweakClass != null) {
            programArgs.append("--tweakClass").append(" ").append(extension.tweakClass).append(" ");
        }

        programArgs.append("--assetsDir").append(" ").append(GameUtil.getClientAssetDir(extension)).append(" ");
        programArgs.append("--assetIndex").append(" ").append(extension.gameVersion).append(" ");
        programArgs.append("--version").append(" ").append("ML4G").append(" ");
        programArgs.append("--accessToken").append(" ").append("0").append(" ");

        try {
            String idea = IOUtils.toString(Objects.requireNonNull(GenIdeaRunTask.class.getResourceAsStream("/idea_run_config_template.xml")), StandardCharsets.UTF_8);
            idea = idea.replace("%NAME%", "ML4G Client Run");
            idea = idea.replace("%MAIN_CLASS%", extension.mainClass);
            idea = idea.replace("%IDEA_MODULE%", getModule());
            idea = idea.replace("%PROGRAM_ARGS%", programArgs.toString().replaceAll("\"", "&quot;"));
            idea = idea.replace("%VM_ARGS%", vmArgs.toString().replaceAll("\"", "&quot;"));

            String projectPath = getProject() == getProject().getRootProject() ? "" : getProject().getPath().replace(':', '_');
            File projectDir = getProject().getRootProject().file(".idea");
            File runConfigsDir = new File(projectDir, "runConfigurations");
            File clientRunConfigs = new File(runConfigsDir, "ML4G_Client_Run" + projectPath + ".xml");
            if (!runConfigsDir.exists()) {
                runConfigsDir.mkdirs();
            }
            FileUtils.write(clientRunConfigs, idea, StandardCharsets.UTF_8);
            File runDir = getProject().getRootProject().file("run");
            if (!runDir.exists()) {
                runDir.mkdir();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getModule() {
        Project project = getProject();
        StringBuilder module = new StringBuilder(project.getName() + ".main");

        while ((project = project.getParent()) != null) {
            module.insert(0, project.getName() + ".");
        }
        return module.toString();
    }
}
