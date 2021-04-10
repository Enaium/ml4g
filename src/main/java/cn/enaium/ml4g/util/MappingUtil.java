package cn.enaium.ml4g.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * @author Enaium
 */
public class MappingUtil {
    public static final HashMap<String, String> map = new HashMap<>();
    public static final HashMap<String, ArrayList<String>> superHashMap = new HashMap<>();

    public static final HashMap<String, String> classObfToCleanMap = new HashMap<>();
    public static final HashMap<String, String> classCleanToObfMap = new HashMap<>();

    public static final HashMap<String, String> fieldObfToCleanMap = new HashMap<>();
    public static final HashMap<String, String> fieldCleanToObfMap = new HashMap<>();

    public static final HashMap<String, String> methodObfToCleanMap = new HashMap<>();
    public static final HashMap<String, String> methodCleanToObfMap = new HashMap<>();

    private static final String NAME_LINE = "^.+:";
    private static final String SPLITTER = "( |->)+";

    public static void initMapping(File mappingFile) throws IOException {
        String text = FileUtils.readFileToString(mappingFile, StandardCharsets.UTF_8);
        {
            String[] lines = text.split("\\r\\n|\\n");
            for (String line : lines) {
                if (line.startsWith("#"))
                    continue;

                //class
                if (line.matches(NAME_LINE)) {
                    String[] split = line.split(SPLITTER);
                    String clean = internalize(split[0]);
                    String obf = internalize(split[1]);
                    obf = obf.substring(0, obf.indexOf(':'));
                    classObfToCleanMap.put(obf, clean);
                    classCleanToObfMap.put(clean, obf);
                }
            }
        }

        {
            String[] lines = text.split("\\r\\n|\\n");
            String currentObfClass = null;
            String currentCleanClass = null;
            for (String line : lines) {
                if (line.startsWith("#"))
                    continue;

                if (line.matches(NAME_LINE)) {
                    currentObfClass = line.substring(line.lastIndexOf(" ") + 1, line.indexOf(":"));
                    currentCleanClass = classObfToCleanMap.getOrDefault(currentObfClass, internalize(currentObfClass));
                    continue;
                }

                if (currentObfClass == null)
                    continue;

                if (!line.contains("(")) {
                    //Field
                    String[] split = line.trim().split(SPLITTER);
                    String clean = currentCleanClass + "/" + split[1];
                    String obf = currentObfClass + "/" + split[2];
                    fieldObfToCleanMap.put(obf, clean);
                    fieldCleanToObfMap.put(clean, obf);
                } else {
                    //Method
                    String[] split = line.contains(":") ? line.substring(line.lastIndexOf(":") + 1).trim().split(SPLITTER) : line.trim().split(SPLITTER);
                    String cleanReturn = !isPrimitive(split[0]) ? "L" + internalize(split[0]) + ";" : internalize(split[0]);
                    String cleanName = split[1].substring(0, split[1].lastIndexOf("("));
                    String cleanArgs = split[1].substring(split[1].indexOf("(") + 1, split[1].lastIndexOf(")"));
                    String obfReturn = !isPrimitive(split[0]) ? "L" + classCleanToObfMap.getOrDefault(internalize(split[0]), internalize(split[0])) + ";" : cleanReturn;
                    String obfName = split[2];
                    String obfArgs;

                    if (!cleanArgs.equals("")) {
                        StringBuilder tempCleanArs = new StringBuilder();
                        StringBuilder tempObfArs = new StringBuilder();
                        for (String s : cleanArgs.split(",")) {
                            if (!isPrimitive(s)) {
                                tempObfArs.append("L").append(classCleanToObfMap.getOrDefault(internalize(s), internalize(s))).append(";");
                                tempCleanArs.append("L").append(internalize(s)).append(";");
                            } else {
                                tempObfArs.append(internalize(s));
                                tempCleanArs.append(internalize(s));
                            }
                        }
                        obfArgs = "(" + tempObfArs.toString() + ")";
                        cleanArgs = "(" + tempCleanArs.toString() + ")";
                    } else {
                        obfArgs = "()";
                        cleanArgs = "()";
                    }

                    String obf = currentObfClass + "/" + obfName + " " + obfArgs + obfReturn;
                    String clean = currentCleanClass + "/" + cleanName + " " + cleanArgs + cleanReturn;
                    methodObfToCleanMap.put(obf, clean);
                    methodCleanToObfMap.put(clean, obf);
                }
            }
        }
    }

    private static String internalize(String name) {
        switch (name) {
            case "int":
                return "I";
            case "float":
                return "F";
            case "double":
                return "D";
            case "long":
                return "J";
            case "boolean":
                return "Z";
            case "short":
                return "S";
            case "byte":
                return "B";
            case "void":
                return "V";
            default:
                return name.replace('.', '/');
        }
    }

    private static boolean isPrimitive(String name) {
        switch (name) {
            case "int":
            case "float":
            case "double":
            case "long":
            case "boolean":
            case "short":
            case "byte":
            case "void":
                return true;
            default:
                return false;
        }
    }

    public static void putRemap(boolean clean) {
        if (clean) {
            map.putAll(classObfToCleanMap);
        } else {
            map.putAll(classCleanToObfMap);
        }

        fieldObfToCleanMap.forEach((k, v) -> {
            String key = clean ? k : v;
            String value = clean ? v : k;
            String obfClassName = key.substring(0, key.lastIndexOf("/"));
            String obfFieldName = key.substring(key.lastIndexOf("/") + 1);
            map.put(obfClassName + "." + obfFieldName, value.substring(value.lastIndexOf("/") + 1));
        });

        methodObfToCleanMap.forEach((k, v) -> {
            String key = clean ? k : v;
            String value = clean ? v : k;
            String obfLeft = key.split(" ")[0];
            String obfRight = key.split(" ")[1];
            String cleanLeft = value.split(" ")[0];
            String cleanMethodName = cleanLeft.substring(cleanLeft.lastIndexOf("/") + 1);
            String obfClassName = obfLeft.substring(0, obfLeft.lastIndexOf("/"));
            String obfMethodName = obfLeft.substring(obfLeft.lastIndexOf("/") + 1);
            map.put(obfClassName + "." + obfMethodName + obfRight, cleanMethodName);
        });
    }

    public static void analyzeJar(File inputFile) throws IOException {
        JarFile jarFile = new JarFile(inputFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory())
                continue;
            if (!jarEntry.getName().endsWith(".class"))
                continue;

            analyze(IOUtils.toByteArray(jarFile.getInputStream(jarEntry)));

        }
        jarFile.close();
    }

    public static void analyze(byte[] bytes) throws IOException {
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(new ClassVisitor(ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (!superName.startsWith("java/")) {
                    if (superHashMap.containsKey(name)) {
                        if (!superHashMap.get(name).contains(superName)) {
                            superHashMap.get(name).add(superName);
                        }
                    } else {
                        superHashMap.put(name, new ArrayList<>(Collections.singleton(superName)));
                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        }, 0);
    }

    public static void cleanJar(File inputFile, File outFile) throws IOException {
        analyzeJar(inputFile);
        ZipOutputStream jarOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)), StandardCharsets.UTF_8);
        JarFile jarFile = new JarFile(inputFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory())
                continue;
            if (jarEntry.getName().endsWith(".class")) {
                String name = map.get(jarEntry.getName().replace(".class", ""));
                if (name != null) {
                    name += ".class";
                } else {
                    name = jarEntry.getName();
                }
                jarOutputStream.putNextEntry(new JarEntry(name));
                acceptClass(jarFile.getInputStream(jarEntry), jarOutputStream);
            } else {
                if (jarEntry.getName().endsWith("MANIFEST.MF"))
                    continue;

                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                IOUtils.copy(jarFile.getInputStream(jarEntry), jarOutputStream);
            }
            jarOutputStream.closeEntry();
        }
        jarFile.close();
        jarOutputStream.close();
    }

    private static void acceptClass(InputStream input, OutputStream output) throws IOException {
        output.write(accept(IOUtils.toByteArray(input)));
        output.flush();
    }

    public static byte[] accept(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM9, classWriter) {
        }, new SimpleRemapper(map) {
            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name);
                if (remappedName == null) {
                    if (superHashMap.get(owner) != null) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapFieldName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }
                return remappedName == null ? name : remappedName;
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name + descriptor);
                if (remappedName == null) {
                    if (superHashMap.get(owner) != null) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapMethodName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }
                return remappedName == null ? name : remappedName;
            }
        });
        classReader.accept(classRemapper, 0);
        return classWriter.toByteArray();
    }
}
