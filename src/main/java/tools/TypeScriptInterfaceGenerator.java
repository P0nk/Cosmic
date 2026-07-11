package tools;

import client.inventory.MakerService;
import constants.inventory.ItemConstants;
import javassist.*;
import javassist.bytecode.*;
import scripting.event.EventInstanceManager;
import scripting.event.EventScriptManager;
import scripting.item.ItemScriptManager;
import scripting.map.MapScriptManager;
import scripting.npc.NPCConversationManager;
import scripting.portal.PortalScriptManager;
import scripting.quest.QuestScriptManager;
import server.ItemInformationProvider;
import server.MakerItemFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class TypeScriptInterfaceGenerator {
    static Set<Class<?>> existingDefinitions = new HashSet<>();
    static Set<Class<?>> definitionQueue = new HashSet<>();
    static Set<String> existingClassNames = new HashSet<>();
    static boolean queueStarted = false;

    private static <T> boolean isLibrary(Class<T> clazz) {
        return !clazz.getName().startsWith("client.")
                && !clazz.getName().startsWith("config.")
                && !clazz.getName().startsWith("constants.")
                && !clazz.getName().startsWith("MapleStory.")
                && !clazz.getName().startsWith("net.")
                && !clazz.getName().startsWith("provider.")
                && !clazz.getName().startsWith("scripting.")
                && !clazz.getName().startsWith("server.")
                && !clazz.getName().startsWith("tools.");
    }

    private static <T> boolean isOdin(Class<T> clazz) {
        return clazz.getName().startsWith("client.")
                || clazz.getName().startsWith("config.")
                || clazz.getName().startsWith("constants.")
                || clazz.getName().startsWith("MapleStory.")
                || clazz.getName().startsWith("net.")
                || clazz.getName().startsWith("provider.")
                || clazz.getName().startsWith("scripting.")
                || clazz.getName().startsWith("server.")
                || clazz.getName().startsWith("tools.");
    }

    public static <T> String generateInterface(Class<T> clazz, boolean force) {
        if (!force & isLibrary(clazz)) {
            definitionQueue.remove(clazz);
            return "";
        }

        if (definitionQueue.contains(clazz) || existingDefinitions.contains(clazz)) {
            definitionQueue.remove(clazz);
            return "";
        }
        existingDefinitions.add(clazz);

        System.out.println(String.format("Generating interface for %s", clazz.getName()));

        StringBuilder sb = new StringBuilder();

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class) && !superClass.equals(Enum.class)) {
            if (!definitionQueue.contains(superClass) && !existingDefinitions.contains(superClass)) {
                definitionQueue.add(superClass);
            }
        }

        boolean isJavaLib = clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.");
        String className = clazz.getSimpleName();

        if (existingClassNames.contains(className)) {
            className = clazz.getName().replace(".", "_");
        }
        existingClassNames.add(className);

        sb.append("interface ").append(className);
        if (!isJavaLib && superClass != null && isOdin(superClass) && !superClass.equals(Object.class) && !superClass.equals(Enum.class)) {
            sb.append(" extends ").append(superClass.getSimpleName());
        }
        sb.append(" {\n");

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers())) {
                String fieldName = field.getName();
                String fieldType = getSimpleTypeName(field.getType());
                sb.append("\t").append(fieldName).append(": ").append(fieldType).append(";\n");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        Set<String> methodNames = new HashSet<>();
        for (Method method : methods) {
            if (Arrays.stream(fields).anyMatch(field -> field.getName().equals(method.getName()))) continue;

            if (Modifier.isPublic(method.getModifiers())) {
                String signature = generateMethodSignature(method);
                if (!methodNames.contains(signature)) {
                    methodNames.add(signature);
                    sb.append(signature).append(";\n");
                }
            }
        }

        sb.append("}\n");

        if (!queueStarted) {
            queueStarted = true;
            sb.append(handleDefinitionQueue());
        }

        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (!definitionQueue.contains(innerClass) && !existingDefinitions.contains(innerClass)) {
                definitionQueue.add(innerClass);
            }
        }
        for (Class<?> innerClass : clazz.getClasses()) {
            if (!definitionQueue.contains(innerClass) && !existingDefinitions.contains(innerClass)) {
                definitionQueue.add(innerClass);
            }
        }

        return sb.toString();
    }

    private static String handleDefinitionQueue() {
        StringBuilder sb = new StringBuilder();
        while (!definitionQueue.isEmpty()) {
            Class<?> clazz = definitionQueue.iterator().next();
            definitionQueue.remove(clazz);
            sb.append(generateInterface(clazz, false));
        }
        return sb.toString();
    }

    private static String generateMethodSignature(Method method) {
//        System.out.println(String.format("Generating method signature for %s", method.getName()));

        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(method.getName()).append("(");

        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameterNames = getArgumentNames(method);

        boolean parameterNamesAreValid = true;
        Set<String> parameterNamesSet = new HashSet<>();
        for (String parameterName : parameterNames) {
            if (parameterNamesSet.contains(parameterName)
                    || parameterName.equals("this")
                    || parameterName.equals("super")
                    || parameterName.equals("null")
                    || parameterName.equals("undefined")
                    || parameterName.equals("true")
                    || parameterName.equals("false")
                    || parameterName.equals("in")
                    || parameterName.equals("of")
            ) {
                parameterNamesAreValid = false;
                break;
            }
            parameterNamesSet.add(parameterName);
        }

        if (!parameterNamesAreValid) {
            parameterNames.clear();
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterNames.add("arg" + i);
            }
        } else {
            for (int i = 0; i < parameterTypes.length; i++) {
                try {
                    Class<?> parameterType = parameterTypes[i];
                    String parameterName = parameterNames.get(i);
                    sb.append(parameterName).append(": ").append(getSimpleTypeName(parameterType));
                    if (i < parameterTypes.length - 1) {
                        sb.append(", ");
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        sb.append("): ").append(getSimpleTypeName(method.getReturnType()));

        return sb.toString();
    }

    private static List<String> getArgumentNames(Method method) {
        List<String> argumentNames = new ArrayList<>();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className);
            CtMethod ctMethod = ctClass.getDeclaredMethod(methodName, toCtClasses(parameterTypes));
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (localVariableAttribute != null) {
                int parameterCount = parameterTypes.length;
                int startIndex = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
                for (int i = startIndex; i < parameterCount + startIndex; i++) {
                    argumentNames.add(localVariableAttribute.variableName(i));
                }
            }
        } catch (Exception e) {
            // ignore and return empty list
        }
        return argumentNames;
    }

    private static CtClass[] toCtClasses(Class<?>[] classes) throws NotFoundException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass[] ctClasses = new CtClass[classes.length];
        for (int i = 0; i < classes.length; i++) {
            ctClasses[i] = classPool.get(classes[i].getName());
        }
        return ctClasses;
    }

    private static String getSimpleTypeName(Class<?> clazz) {
        if (clazz == null) return null;

        if (clazz == void.class) {
            return "void";
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            return "boolean";
        } else if (clazz == char.class || clazz == String.class) {
            return "string";
        } else if (clazz == byte.class || clazz == short.class || clazz == int.class || clazz == long.class || clazz == Byte.class || clazz == Short.class || clazz == Integer.class || clazz == Long.class) {
            return "number";
        } else if (clazz == float.class || clazz == double.class || clazz == Float.class || clazz == Double.class) {
            return "number";
        } else if (clazz == Object.class) {
            return "object";
        } else if (clazz.isArray() || clazz == List.class || clazz == Collection.class || clazz == ArrayList.class) {
            Class<?> componentType = clazz.getComponentType();
            return getSimpleTypeName(componentType) + "[]";
        } else {
            if (!definitionQueue.contains(clazz) && !existingDefinitions.contains(clazz)) {
                definitionQueue.add(clazz);
            }
            if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.")) {
                return clazz.getName();
            }
            return clazz.getSimpleName();
        }
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                declare namespace java {
                    namespace lang {
                        type Enum = any;
                        type Runnable = any;
                        type Throwable = any;
                        type Integer = any;
                        type Short = any;
                    }
                    namespace util {
                        type Map = any;
                        type Set = any;
                        type List = any;
                        type Iterator = any;
                        type Collection = any;
                        type HashMap = any;
                        type ArrayList = any;
                        type LinkedList = any;
                        type HashSet = any;
                        type TreeSet = any;
                        type Stack = any;
                        type Vector = any;
                        type Calendar = any;
                        type Optional = any;
                        
                        namespace concurrent {
                            type ScheduledFuture = any;
                        }
                        
                        namespace regex {
                            type Pattern = any;
                            type Matcher = any;
                        }
                    }
                    namespace io {}
                    namespace net {}
                    namespace sql {
                        type Connection = any;
                        type PreparedStatement = any;
                        type ResultSet = any;
                        type Timestamp = any;
                    }
                    namespace awt {
                        interface Point {
                            x: number;
                            y: number;
                            equals(): boolean;
                            toString(): string;
                            getLocation(): java.awt.Point;
                            move(x: number, y: number): void;
                            getX(): number;
                            getY(): number;
                            setLocation(p: java.awt.Point): void;
                            setLocation(x: number, y: number): void;
                            translate(dx: number, dy: number): void;
                        }
                        
                        interface Rectangle {
                            x: number;
                            y: number;
                            width: number;
                            height: number;
                            equals(): boolean;
                            toString(): string;
                            getLocation(): java.awt.Point;
                            getSize(): java.awt.Dimension;
                            move(x: number, y: number): void;
                            resize(width: number, height: number): void;
                            getX(): number;
                            getY(): number;
                            getWidth(): number;
                            getHeight(): number;
                            setLocation(p: java.awt.Point): void;
                            setLocation(x: number, y: number): void;
                            setSize(d: java.awt.Dimension): void;
                            setSize(width: number, height: number): void;
                            contains(p: java.awt.Point): boolean;
                            contains(x: number, y: number): boolean;
                            contains(r: java.awt.Rectangle): boolean;
                            intersects(r: java.awt.Rectangle): boolean;
                            union(r: java.awt.Rectangle): java.awt.Rectangle;
                            add(p: java.awt.Point): void;
                            add(r: java.awt.Rectangle): void;
                            subtract(r: java.awt.Rectangle): void;
                            translate(dx: number, dy: number): void; 
                        }
                        
                        interface Dimension {
                            width: number;
                            height: number;
                            equals(): boolean;
                            toString(): string;
                            getSize(): java.awt.Dimension;
                            resize(width: number, height: number): void;
                            getWidth(): number;
                            getHeight(): number;
                            setSize(d: java.awt.Dimension): void;
                            setSize(width: number, height: number): void;
                            contains(p: java.awt.Point): boolean;
                            contains(x: number, y: number): boolean;
                            contains(r: java.awt.Rectangle): boolean;
                            intersects(r: java.awt.Rectangle): boolean;
                            union(r: java.awt.Rectangle): java.awt.Rectangle;
                            add(p: java.awt.Point): void;
                            add(r: java.awt.Rectangle): void;
                            subtract(r: java.awt.Rectangle): void;
                            translate(dx: number, dy: number): void; 
                        }
                    }
                    namespace security {}
                    namespace text {}
                    namespace nio {}
                    namespace math {}
                    namespace time {
                        type Instant = any;
                        type Duration = any;
                    }
                }
                declare namespace javax {
                    namespace script {
                        type Invocable = any;
                        type ScriptEngine = any;
                    }
                }
                type ChannelHandlerContext = any;
                \n    
                """);

        sb.append(TypeScriptInterfaceGenerator.generateInterface(MakerItemFactory.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(MakerItemFactory.MakerItemCreateEntry.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(MakerService.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(NPCConversationManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(ItemScriptManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(MapScriptManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(PortalScriptManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(QuestScriptManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(EventScriptManager.class, false));
        sb.append(TypeScriptInterfaceGenerator.generateInterface(EventInstanceManager.class, false));

        sb.append("\n\n");
        sb.append("declare var cm: NPCConversationManager;\n");
        sb.append("declare var im: NPCConversationManager;\n");
        sb.append("declare var ms: MapScriptManager;\n");
        sb.append("declare var pi: PortalScriptManager;\n");
        sb.append("declare var qm: QuestScriptManager;\n");
        sb.append("declare var em: EventScriptManager;\n");
        sb.append("declare var eim: EventInstanceManager;");

//        System.out.println(sb);

        try {
            FileWriter writer = new FileWriter("scripts/lib_.d.ts", false);
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}