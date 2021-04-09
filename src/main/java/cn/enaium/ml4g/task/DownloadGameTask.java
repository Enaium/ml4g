package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import cn.enaium.ml4g.util.GameUtil;
import javafx.scene.control.ProgressBar;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * @author Enaium
 */
public class DownloadGameTask extends Task {
    @TaskAction
    public void downloadGame() {
        File clientJar = GameUtil.getClientFile(extension);
        try {
            if (!GameUtil.fileVerify(clientJar, GameUtil.getClientJarSha1(extension.gameVersion))) {
                File localJar = GameUtil.getLocalJar(extension.gameVersion);
                if (GameUtil.fileVerify(localJar, GameUtil.getClientJarSha1(extension.gameVersion))) {
                    FileUtils.copyFile(localJar, clientJar);
                } else {
                    FileUtils.writeByteArrayToFile(clientJar, GameUtil.getClientJar(extension.gameVersion));
                }
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }
    }
}
