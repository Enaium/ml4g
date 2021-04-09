package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import org.gradle.api.DefaultTask;

/**
 * @author Enaium
 */
public class Task extends DefaultTask {

    protected final ML4GExtension extension;

    public Task() {
        extension = getProject().getExtensions().getByType(ML4GExtension.class);
    }
}
