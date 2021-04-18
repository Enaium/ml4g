package cn.enaium.ml4g.task;

import cn.enaium.ml4g.task.mapping.InjectMapping;
import cn.enaium.ml4g.task.mapping.MixinMapping;
import cn.enaium.ml4g.util.GameUtil;
import cn.enaium.ml4g.util.MappingUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Enaium
 */
public class RemappingClassTask extends Task {
    @TaskAction
    public void remapping() {
        try {
            JsonObject mixinReMap = new JsonObject();
            JsonObject mixinMappings = new JsonObject();
            JsonObject injectMappings = new JsonObject();
            File classes = new File(getProject().getBuildDir(), "classes");
            MappingUtil.analyzeJar(GameUtil.getClientCleanFile(extension));

            MappingUtil.initMapping(GameUtil.getClientMappingFile(extension));
            MappingUtil.putRemap(false);

            if (extension.mixinRefMap != null) {
                for (File file : FileUtils.listFiles(classes.getAbsoluteFile(), new String[]{"class"}, true)) {
                    byte[] bytes = FileUtils.readFileToByteArray(file);
                    MixinMapping mixinMapping = new MixinMapping();
                    mixinMapping.accept(bytes);
                    MappingUtil.superHashMap.put(mixinMapping.className, new ArrayList<>(mixinMapping.mixins));
                    for (String mixin : mixinMapping.mixins) {

                        JsonObject mapping = new JsonObject();

                        mixinMapping.methods.forEach((descriptor, methods) -> {
                            for (String method : methods) {
                                if (method.contains("(")) {
                                    mapping.addProperty(method, getMethodObf(mixin, method, false));
                                } else {
                                    mapping.addProperty(method, getMethodObf(mixin, method + descriptor.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", ""), false));
                                }
                            }
                        });

                        for (String mixinTarget : mixinMapping.targets) {
                            if (!mixinTarget.contains("field:")) {
                                String targetClass = mixinTarget.substring(1, mixinTarget.indexOf(";"));

                                String targetMethod = getMethodObf(targetClass, mixinTarget.substring(mixinTarget.indexOf(";") + 1), false);
                                if (targetMethod == null) {
                                    continue;
                                }
                                mapping.addProperty(mixinTarget, targetMethod);
                            } else {
                                String left = mixinTarget.split("field:")[0];
                                String right = mixinTarget.split("field:")[1];
                                String targetClass = MappingUtil.classCleanToObfMap.get(left.substring(1, left.indexOf(";")));
                                String targetField = MappingUtil.classCleanToObfMap.get(right.substring(1, right.indexOf(";")));

                                if (targetClass == null || targetField == null) {
                                    continue;
                                }

                                mapping.addProperty(mixinTarget, "L" + targetClass + ";field:L" + targetField + ";");
                            }
                        }

                        for (Map.Entry<String, String> entry : mixinMapping.accessors.entrySet()) {

                            String fieldName = MappingUtil.fieldCleanToObfMap.get(mixin + "/" + entry.getValue());

                            if (fieldName == null) {
                                continue;
                            }

                            if (entry.getKey().contains(";")) {
                                String arg;
                                if (!entry.getKey().contains(")V")) {
                                    arg = entry.getKey().substring(entry.getKey().lastIndexOf(")") + 1);
                                } else {
                                    arg = entry.getKey().substring(entry.getKey().indexOf("(") + 1, entry.getKey().lastIndexOf(")"));
                                }

                                arg = arg.substring(1, arg.lastIndexOf(";"));
                                arg = MappingUtil.classCleanToObfMap.get(arg);
                                if (arg == null) {
                                    continue;
                                }
                                mapping.addProperty(entry.getValue(), fieldName.split("/")[1] + ":L" + arg + ";");
                            } else {
                                mapping.addProperty(entry.getValue(), entry.getKey());
                            }
                        }

                        for (Map.Entry<String, String> entry : mixinMapping.invokes.entrySet()) {
                            mapping.addProperty(entry.getValue(), getMethodObf(mixin, entry.getValue() + entry.getKey(), false));
                        }

                        mixinMappings.add(mixinMapping.className, mapping);
                    }
                }
                mixinReMap.add("mappings", mixinMappings);
            }

            if (extension.injectRemapping != null) {
                for (File file : FileUtils.listFiles(classes.getAbsoluteFile(), new String[]{"class"}, true)) {
                    byte[] bytes = FileUtils.readFileToByteArray(file);
                    InjectMapping injectMapping = new InjectMapping();
                    injectMapping.accept(bytes);
                    MappingUtil.superHashMap.put(injectMapping.className, new ArrayList<>(injectMapping.injects));
                    for (String inject : injectMapping.injects) {
                        JsonObject mapping = new JsonObject();
                        String obfClassName = MappingUtil.classCleanToObfMap.get(inject.replace(".", "/"));
                        mapping.addProperty(inject, obfClassName);

                        for (Map.Entry<String, String> entry : injectMapping.methods.entrySet()) {
                            mapping.addProperty(entry.getValue(), getMethodObf(inject.replace(".", "/"), entry.getValue() + entry.getKey().replace("Lcn/enaium/inject/callback/Callback;", ""), true));
                        }

                        for (String target : injectMapping.targets) {
                            String targetClass = target.substring(1, target.indexOf(";"));

                            String targetMethod = getMethodObf(targetClass, target.substring(target.indexOf(";") + 1), false);

                            System.out.println(targetMethod);

                            if (targetMethod == null) {
                                continue;
                            }


                            mapping.addProperty(target, targetMethod);
                        }

                        injectMappings.add(injectMapping.className, mapping);
                    }
                }
            }

            JavaPluginConvention java = (JavaPluginConvention) getProject().getConvention().getPlugins().get("java");
            File resourceDir = new File(getProject().getBuildDir(), "resources");
            for (SourceSet sourceSet : java.getSourceSets()) {
                if (!resourceDir.exists()) {
                    resourceDir.mkdir();
                }
                File dir = new File(resourceDir, sourceSet.getName());
                if (!dir.exists()) {
                    dir.mkdir();
                }

                if (extension.mixinRefMap != null) {
                    FileUtils.write(new File(dir, extension.mixinRefMap), new GsonBuilder().setPrettyPrinting().create().toJson(mixinReMap), StandardCharsets.UTF_8);
                }

                if (extension.injectRemapping != null) {
                    FileUtils.write(new File(dir, extension.injectRemapping), new GsonBuilder().setPrettyPrinting().create().toJson(injectMappings), StandardCharsets.UTF_8);
                }
            }


            //class
            for (File file : FileUtils.listFiles(classes.getAbsoluteFile(), new String[]{"class"}, true)) {
                byte[] bytes = FileUtils.readFileToByteArray(file);
                MappingUtil.analyze(bytes);
                FileUtils.writeByteArrayToFile(file, MappingUtil.accept(bytes));
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }
    }

    private String getMethodObf(String klass, String method, boolean only) {
        String methodName = method.substring(0, method.indexOf("("));
        String methodDescriptor = method.substring(method.indexOf("("));
        String methodObf = MappingUtil.methodCleanToObfMap.get(klass + "/" + methodName + " " + methodDescriptor);
        if (methodObf == null) {
            return null;
        }
        if (!only) {
            methodObf = "L" + methodObf.split(" ")[0].replace("/", ";") + methodObf.split(" ")[1];
        } else {
            methodObf = methodObf.split(" ")[0];
            methodObf = methodObf.substring(methodObf.lastIndexOf("/") + 1);
        }
        return methodObf;
    }
}
