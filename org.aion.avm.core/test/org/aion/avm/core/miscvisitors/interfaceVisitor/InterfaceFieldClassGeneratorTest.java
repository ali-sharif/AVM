package org.aion.avm.core.miscvisitors.interfaceVisitor;

import org.aion.avm.core.ClassToolchain;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.core.miscvisitors.InterfaceFieldClassGeneratorVisitor;
import org.aion.avm.core.miscvisitors.InterfaceFieldNameMappingVisitor;
import org.aion.avm.core.types.GeneratedClassConsumer;
import org.aion.avm.core.util.Helpers;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InterfaceFieldClassGeneratorTest {

    @Test
    public void testInterfaceContainingFieldsInnerClass() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String[] classNames = new String[]{getClassName(ClassWithFields.class),
                getClassName(ClassWithFields.InnerInterface.class),
                getClassName(ClassWithFields.InnerInterface.FIELDS.class),
                getClassName(OuterInterfaceWithFieldsClass.class),
                getClassName(OuterInterfaceWithFieldsClass.FIELDS.class)
        };

        Map<String, byte[]> classes = new HashMap<>();

        GeneratedClassConsumer consumer = (superClassName, className, bytecode) -> {
            classes.put(Helpers.internalNameToFulllyQualifiedName(className), bytecode);
        };
        Map<String, String> interfaceFieldClassNames = new HashMap<>();
        String javaLangObjectSlashName = "java/lang/Object";

        byte[][] bytecode = new byte[classNames.length][];
        for (int i = 0; i < classNames.length; ++i) {
            bytecode[i] = Helpers.loadRequiredResourceAsBytes(classNames[i] + ".class");
        }

        Function<byte[], byte[]> transformer = (inputBytes) ->
                new ClassToolchain.Builder(inputBytes, 0)
                        .addNextVisitor(new InterfaceFieldClassGeneratorVisitor(consumer, interfaceFieldClassNames, javaLangObjectSlashName))
                        .addNextVisitor(new InterfaceFieldNameMappingVisitor(interfaceFieldClassNames))
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();

        for (int i = 0; i < classNames.length; ++i) {
            classes.put(Helpers.internalNameToFulllyQualifiedName(classNames[i]), transformer.apply(bytecode[i]));
        }

        //ensure FIELDS class is generated for all the interface
        Assert.assertEquals(7, classes.size());

        // ensure fields are removed from the class
        AvmClassLoader loader = NodeEnvironment.singleton.createInvocationClassLoader(classes);
        Class<?> clazz = loader.loadClass(OuterInterfaceWithFieldsClass.class.getName());
        Assert.assertEquals(0, clazz.getDeclaredFields().length);

        // validate field values
        clazz = loader.loadClass(ClassWithFields.InnerInterface.class.getName() + "$FIELDS");
        Assert.assertNotNull(clazz.getDeclaredField("d"));
        Assert.assertNotNull(clazz.getDeclaredField("e"));

        clazz = loader.loadClass(ClassWithFields.InnerInterface.class.getName() + "$FIELDS0");
        Field a = clazz.getField("a");
        a.setAccessible(true);
        Assert.assertEquals(1, a.get(null));

        Field b = clazz.getField("b");
        b.setAccessible(true);
        Assert.assertEquals("abc", b.get(null));

        Field c = clazz.getField("c");
        c.setAccessible(true);
        Assert.assertEquals(Object.class, c.get(null).getClass());

        clazz = loader.loadClass(OuterInterfaceWithFieldsClass.class.getName() + "$FIELDS0");
        Field outerA = clazz.getField("a");
        outerA.setAccessible(true);
        Assert.assertEquals(1, outerA.get(null));

        clazz = loader.loadClass(OuterInterfaceWithFieldsClass.class.getName() + "$FIELDS");
        Assert.assertNotNull(clazz.getDeclaredField("f"));

        clazz = loader.loadClass(ClassWithFields.class.getName());
        Object ret = clazz.getMethod("f").invoke(null);
        Assert.assertEquals(4, ret);
    }

    @Test
    public void testNestedInterface() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String[] classNames = new String[]{getClassName(NestedInterfaces.class),
                getClassName(NestedInterfaces.InnerInterface.class),
                getClassName(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.class),
                getClassName(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.InnerInterfaceLevel3.class)
        };

        Map<String, byte[]> classes = new HashMap<>();

        GeneratedClassConsumer consumer = (superClassName, className, bytecode) -> {
            classes.put(Helpers.internalNameToFulllyQualifiedName(className), bytecode);
        };
        Map<String, String> interfaceFieldClassNames = new HashMap<>();
        String javaLangObjectSlashName = "java/lang/Object";

        byte[][] bytecode = new byte[classNames.length][];
        for (int i = 0; i < classNames.length; ++i) {
            bytecode[i] = Helpers.loadRequiredResourceAsBytes(classNames[i] + ".class");
        }

        Function<byte[], byte[]> transformer = (inputBytes) ->
                new ClassToolchain.Builder(inputBytes, 0)
                        .addNextVisitor(new InterfaceFieldClassGeneratorVisitor(consumer, interfaceFieldClassNames, javaLangObjectSlashName))
                        .addNextVisitor(new InterfaceFieldNameMappingVisitor(interfaceFieldClassNames))
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();

        for (int i = 0; i < classNames.length; ++i) {
            classes.put(Helpers.internalNameToFulllyQualifiedName(classNames[i]), transformer.apply(bytecode[i]));
        }

        //ensure FIELDS class is generated for all the interface
        Assert.assertEquals(7, classes.size());

        // ensure fields are removed from the class
        AvmClassLoader loader = NodeEnvironment.singleton.createInvocationClassLoader(classes);
        Class<?> clazz = loader.loadClass(NestedInterfaces.InnerInterface.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        clazz = loader.loadClass(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        clazz = loader.loadClass(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.InnerInterfaceLevel3.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        // validate field values
        clazz = loader.loadClass(NestedInterfaces.InnerInterface.class.getName() + "$FIELDS");
        Assert.assertNotNull(clazz.getDeclaredField("a"));
        Assert.assertNotNull(clazz.getDeclaredField("b"));

        clazz = loader.loadClass(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.class.getName() + "$FIELDS");
        Assert.assertNotNull(clazz.getDeclaredField("a"));
        Assert.assertNotNull(clazz.getDeclaredField("c"));

        clazz = loader.loadClass(NestedInterfaces.InnerInterface.InnerInterfaceLevel2.InnerInterfaceLevel3.class.getName() + "$FIELDS");
        Assert.assertNotNull(clazz.getDeclaredField("a"));
        Assert.assertNotNull(clazz.getDeclaredField("d"));

        clazz = loader.loadClass(NestedInterfaces.class.getName());
        Object ret = clazz.getMethod("f").invoke(null);
        Assert.assertEquals(18, ret);
    }

    @Test
    public void testLowerLevelFieldsInInterface() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        String[] classNames = new String[]{getClassName(ClassWithLowerLevelOfField.class),
                getClassName(ClassWithLowerLevelOfField.InnerInterface.class),
                getClassName(ClassWithLowerLevelOfField.InnerInterface.innerClass.class),
                getClassName(ClassWithLowerLevelOfField.InnerInterface.innerClass.FIELDS.class),
                getClassName(OuterInterfaceFields.class),
                getClassName(OuterInterfaceFields.innerClass.class),
                getClassName(OuterInterfaceFields.innerClass.FIELDS.class)
        };

        Map<String, byte[]> classes = new HashMap<>();

        GeneratedClassConsumer consumer = (superClassName, className, bytecode) -> {
            classes.put(Helpers.internalNameToFulllyQualifiedName(className), bytecode);
        };
        Map<String, String> interfaceFieldClassNames = new HashMap<>();
        String javaLangObjectSlashName = "java/lang/Object";

        byte[][] bytecode = new byte[classNames.length][];
        for (int i = 0; i < classNames.length; ++i) {
            bytecode[i] = Helpers.loadRequiredResourceAsBytes(classNames[i] + ".class");
        }

        Function<byte[], byte[]> transformer = (inputBytes) ->
                new ClassToolchain.Builder(inputBytes, 0)
                        .addNextVisitor(new InterfaceFieldClassGeneratorVisitor(consumer, interfaceFieldClassNames, javaLangObjectSlashName))
                        .addNextVisitor(new InterfaceFieldNameMappingVisitor(interfaceFieldClassNames))
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();

        for (int i = 0; i < classNames.length; ++i) {
            classes.put(Helpers.internalNameToFulllyQualifiedName(classNames[i]), transformer.apply(bytecode[i]));
        }

        //ensure FIELDS class is generated for all the interface
        Assert.assertEquals(9, classes.size());

        // ensure fields are removed from the class
        AvmClassLoader loader = NodeEnvironment.singleton.createInvocationClassLoader(classes);
        Class<?> clazz = loader.loadClass(ClassWithLowerLevelOfField.InnerInterface.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        clazz = loader.loadClass(OuterInterfaceFields.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        // validate field values
        clazz = loader.loadClass(ClassWithLowerLevelOfField.InnerInterface.class.getName() + "$FIELDS");
        Field a = clazz.getField("a");
        a.setAccessible(true);
        Assert.assertEquals(1, a.get(null));

        Field b = clazz.getField("b");
        b.setAccessible(true);
        Assert.assertEquals("abc", b.get(null));

        clazz = loader.loadClass(OuterInterfaceFields.class.getName() + "$FIELDS");
        a = clazz.getField("a");
        a.setAccessible(true);
        Assert.assertEquals(2, a.get(null));

        b = clazz.getField("b");
        b.setAccessible(true);
        Assert.assertEquals("def", b.get(null));
    }

    @Test
    public void testMultipleFieldsClassNames() throws ClassNotFoundException, NoSuchFieldException {
        String[] classNames = new String[]{getClassName(ClassWithMultipleFieldSuffix.class),
                getClassName(ClassWithMultipleFieldSuffix.InnerInterface.class),
        };

        Map<String, byte[]> classes = new HashMap<>();

        GeneratedClassConsumer consumer = (superClassName, className, bytecode) -> {
            classes.put(Helpers.internalNameToFulllyQualifiedName(className), bytecode);
        };
        Map<String, String> interfaceFieldClassNames = new HashMap<>();
        String javaLangObjectSlashName = "java/lang/Object";

        byte[][] bytecode = new byte[classNames.length][];
        for (int i = 0; i < classNames.length; ++i) {
            bytecode[i] = Helpers.loadRequiredResourceAsBytes(classNames[i] + ".class");
        }

        Function<byte[], byte[]> transformer = (inputBytes) ->
                new ClassToolchain.Builder(inputBytes, 0)
                        .addNextVisitor(new InterfaceFieldClassGeneratorVisitor(consumer, interfaceFieldClassNames, javaLangObjectSlashName))
                        .addNextVisitor(new InterfaceFieldNameMappingVisitor(interfaceFieldClassNames))
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();

        for (int i = 0; i < classNames.length; ++i) {
            classes.put(Helpers.internalNameToFulllyQualifiedName(classNames[i]), transformer.apply(bytecode[i]));
        }

        //ensure FIELDS class is generated for all the interface
        Assert.assertEquals(3, classes.size());

        AvmClassLoader loader = NodeEnvironment.singleton.createInvocationClassLoader(classes);
        Class<?> clazz = loader.loadClass(ClassWithMultipleFieldSuffix.InnerInterface.class.getName());
        Assert.assertEquals(0, clazz.getFields().length);

        // missing class name
        clazz = loader.loadClass(ClassWithMultipleFieldSuffix.InnerInterface.class.getName() + "$FIELDS16");
        Assert.assertNotNull(clazz.getDeclaredField("a"));
        Assert.assertNotNull(clazz.getDeclaredField("b"));
        Assert.assertNotNull(clazz.getDeclaredField("c"));
    }

    @Test
    public void testNoFields() {
        String[] classNames = new String[]{getClassName(ClassWithNoInterfaceFields.class),
                getClassName(ClassWithNoInterfaceFields.InnerInterface.class),
                getClassName(outerInterfaceNoFields.class),
        };

        Map<String, byte[]> classes = new HashMap<>();

        GeneratedClassConsumer consumer = (superClassName, className, bytecode) -> {
            classes.put(Helpers.internalNameToFulllyQualifiedName(className), bytecode);
        };
        Map<String, String> interfaceFieldClassNames = new HashMap<>();
        String javaLangObjectSlashName = "java/lang/Object";

        byte[][] bytecode = new byte[classNames.length][];
        for (int i = 0; i < classNames.length; ++i) {
            bytecode[i] = Helpers.loadRequiredResourceAsBytes(classNames[i] + ".class");
        }

        Function<byte[], byte[]> transformer = (inputBytes) ->
                new ClassToolchain.Builder(inputBytes, 0)
                        .addNextVisitor(new InterfaceFieldClassGeneratorVisitor(consumer, interfaceFieldClassNames, javaLangObjectSlashName))
                        .addNextVisitor(new InterfaceFieldNameMappingVisitor(interfaceFieldClassNames))
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();

        for (int i = 0; i < classNames.length; ++i) {
            classes.put(Helpers.internalNameToFulllyQualifiedName(classNames[i]), transformer.apply(bytecode[i]));
        }

        Assert.assertEquals(5, classes.size());
    }

    private String getClassName(Class<?> clazz) {
        return clazz.getName().replaceAll("\\.", "/");
    }
}
