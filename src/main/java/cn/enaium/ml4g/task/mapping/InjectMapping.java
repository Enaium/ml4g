package cn.enaium.ml4g.task.mapping;

import cn.enaium.ml4g.util.ASMUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Enaium
 */
public class InjectMapping {

    public final List<String> injects = new ArrayList<>();
    public final HashMap<String, String> methods = new HashMap<>();
    public String className = null;

    public void accept(byte[] basic) {
        ClassReader classReader = new ClassReader(basic);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        if (classNode.invisibleAnnotations == null) {
            return;
        }

        for (AnnotationNode invisibleAnnotation : classNode.invisibleAnnotations) {
            if (invisibleAnnotation.desc.equals("Lcn/enaium/inject/annotation/Inject;")) {
                className = classNode.name;
                Type value = ASMUtil.getAnnotationValue(invisibleAnnotation, "value");
                if (value != null && !value.getDescriptor().equals("Lcn/enaium/inject/annotation/Inject;")) {
                    injects.add(value.getClassName());
                    continue;
                }

                String target = ASMUtil.getAnnotationValue(invisibleAnnotation, "target");
                if (target != null && !target.equals("")) {
                    injects.add(target);
                }
            }
        }

        if (className == null) {
            return;
        }

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.invisibleAnnotations == null) {
                continue;
            }

            for (AnnotationNode invisibleAnnotation : methodNode.invisibleAnnotations) {
                if (invisibleAnnotation.desc.equals("Lcn/enaium/inject/annotation/Method;")) {
                    String name = ASMUtil.getAnnotationValue(invisibleAnnotation, "name");
                    methods.put(methodNode.desc, name);
                }
            }
        }
    }
}
