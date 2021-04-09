package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import cn.enaium.ml4g.util.GameUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Enaium
 */
public class DownloadMappingTask extends Task {
    @TaskAction
    public void downloadMapping() {
        File clientMappingFile = GameUtil.getClientMappingFile(extension);
        try {
            if (!GameUtil.fileVerify(clientMappingFile,GameUtil.getClientMappingSha1(extension.gameVersion))) {
                FileUtils.write(clientMappingFile, GameUtil.getClientMapping(extension.gameVersion), StandardCharsets.UTF_8);
            }
        }catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(),e);
        }
    }
}
