package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import cn.enaium.ml4g.util.GameUtil;
import cn.enaium.ml4g.util.MappingUtil;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * @author Enaium
 */
public class CleanGameTask extends Task {

    @TaskAction
    public void cleanGame() {
        File clientCleanFile = GameUtil.getClientCleanFile(extension);
        File clientFile = GameUtil.getClientFile(extension);
        if (clientFile.exists()) {
            try {
                MappingUtil.initMapping(GameUtil.getClientMappingFile(extension));
                MappingUtil.putRemap(true);
                MappingUtil.cleanJar(clientFile, clientCleanFile);
            } catch (IOException e) {
                getProject().getLogger().lifecycle(e.getMessage(), e);
            }
        }
    }
}
