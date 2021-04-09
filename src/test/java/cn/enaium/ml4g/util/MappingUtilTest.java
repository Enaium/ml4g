package cn.enaium.ml4g.util;

import org.junit.jupiter.api.Test;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Enaium
 */
class MappingUtilTest {

    @Test
    void cleanJar() throws IOException {
        MappingUtil.initMapping(new File(FileSystemView.getFileSystemView().getHomeDirectory(), "1.16.5-client.txt"));
        MappingUtil.putRemap(true);
        MappingUtil.cleanJar(GameUtil.getLocalJar("1.16.5"), new File(System.getProperty("user.dir"), "build" + File.separator + "clean.jar"));
    }
}