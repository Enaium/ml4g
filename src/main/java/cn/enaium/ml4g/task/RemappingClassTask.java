package cn.enaium.ml4g.task;

import cn.enaium.ml4g.util.GameUtil;
import cn.enaium.ml4g.util.MappingUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * @author Enaium
 */
public class RemappingClassTask extends Task {
    @TaskAction
    public void remapping() {
        try {
            JsonObject jsonObject = new JsonObject();
            JsonObject mappings = new JsonObject();
            File classes = new File(getProject().getBuildDir(), "classes");
            MappingUtil.analyzeJar(GameUtil.getClientCleanFile(extension));
            for (File file : FileUtils.listFiles(classes.getAbsoluteFile(), new String[]{"class"}, true)) {
                MappingUtil.initMapping(GameUtil.getClientMappingFile(extension));
                MappingUtil.putRemap(false);
                byte[] bytes = FileUtils.readFileToByteArray(file);
                FileUtils.writeByteArrayToFile(file, MappingUtil.accept(bytes));
                if (extension.mixinRefMap != null) {

                    MixinScannerVisitor mixin = new MixinScannerVisitor();
                    new ClassReader(bytes).accept(mixin, 0);
                    for (String target : mixin.getTargets()) {
                        JsonObject mapping = new JsonObject();
                        if (mixin.getMethods().isEmpty())
                            continue;
                        for (String method : mixin.getMethods()) {
                            String methodName = method.substring(0, method.indexOf("("));
                            String methodDescriptor = method.substring(method.lastIndexOf("("));
                            String methodObf = MappingUtil.methodCleanToObfMap.get(target + "/" + methodName + " " + methodDescriptor);
                            if (methodObf == null) {
                                continue;
                            }
                            methodObf = "L" + methodObf.split(" ")[0].replace("/", ";") + methodObf.split(" ")[1];
                            mapping.addProperty(method, methodObf);
                        }
                        mappings.add(mixin.className, mapping);
                    }

                }
            }

            if (extension.mixinRefMap != null) {
                jsonObject.add("mappings", mappings);

                JavaPluginConvention java = (JavaPluginConvention) getProject().getConvention().getPlugins().get("java");
                File resourceDir = new File(getProject().getBuildDir(), "resources");
                for (SourceSet sourceSet : java.getSourceSets()) {
                    if (!resourceDir.exists()) {
                        resourceDir.mkdir();
                    }
                    File dir = new File(resourceDir, sourceSet.getName());
                    if (dir.exists()) {
                        FileUtils.write(new File(dir, extension.mixinRefMap), new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }
    }

    private static class MixinScannerVisitor extends ClassVisitor {

        private AnnotationNode mixin = null;
        private final List<AnnotationNode> injectList = new ArrayList<>();

        String className;

        MixinScannerVisitor() {
            super(ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                return mixin = new AnnotationNode(descriptor);
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) {
                        AnnotationNode inject = new AnnotationNode(descriptor);
                        injectList.add(inject);
                        return inject;
                    }
                    return super.visitAnnotation(descriptor, visible);
                }
            };
        }

        List<String> getMethods() {

            if (injectList.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> methods = new ArrayList<>();

            for (AnnotationNode annotationNode : injectList) {
                List<String> privateMethod = getAnnotationValue(annotationNode, "method");
                if (privateMethod != null) {
                    methods.addAll(privateMethod);
                }
            }

            return methods;
        }

        List<String> getTargets() {
            if (mixin == null) {
                return new ArrayList<>();
            }

            List<String> targets = new ArrayList<>();
            List<Type> publicTargets = getAnnotationValue(mixin, "value");

            if (publicTargets != null) {
                for (Type type : publicTargets) {
                    targets.add(type.getClassName().replace(".", "/"));
                }
            }

            return targets;
        }

        @SuppressWarnings("unchecked")
        private <T> T getAnnotationValue(AnnotationNode annotationNode, String key) {
            boolean getNextValue = false;

            if (annotationNode.values == null) {
                return null;
            }

            for (Object value : annotationNode.values) {
                if (getNextValue) {
                    return (T) value;
                }
                if (value.equals(key)) {
                    getNextValue = true;
                }
            }

            return null;
        }
    }
}
