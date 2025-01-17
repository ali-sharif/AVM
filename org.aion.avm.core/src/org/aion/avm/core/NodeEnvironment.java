package org.aion.avm.core;

import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.core.classloading.AvmSharedClassLoader;
import org.aion.avm.core.dappreading.LoadedJar;
import org.aion.avm.core.instrument.JCLAndAPIHeapInstanceSize;
import org.aion.avm.core.types.*;
import org.aion.avm.core.util.MethodDescriptorCollector;
import org.aion.avm.utilities.Utilities;
import i.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import p.avm.Address;
import p.avm.AltBn128;
import p.avm.Blockchain;
import p.avm.Result;

/**
 * Represents the long-lived global state of a specific "node" instance.
 * For now, this just contains the AvmSharedClassLoader (since it is stateless and shared by all transactions run on this
 * NodeEnvironment - that is, each AvmImpl instance).
 * Note that this is also responsible for any bootstrap initialization of the shared environment.  Specifically, this involves
 * eagerly loading the shadow JDK in order to run their <clinit> methods.
 */
public class NodeEnvironment {
    // NOTE:  This is only temporarily a singleton and will probably see its relationship inverted, in the future:  becoming the Avm factory.
    public static final NodeEnvironment singleton = new NodeEnvironment();

    private final AvmSharedClassLoader sharedClassLoader;
    // Note that the constant map is a map of constant hashcodes to constant instances.  This is just provided so that reference deserialization
    // mechanisms can map from this primitive identity into the actual instances.
    private final Map<Integer, s.java.lang.Object> constantMap;

    private final Class<?>[] shadowApiClasses;
    // contains all the shadow classes except the exception classes that are generated automatically; used for computing runtime object sizes
    private final Class<?>[] shadowClasses;
    // contains all the supported jcl class names (slash type)
    private final Set<String> jclClassNames;

    public final Map<String, Integer> preRenameRuntimeObjectSizeMap;     // pre-rename; runtime objects including shadow objects, exceptions and API objects
    public final Map<String, Integer> postRenameRuntimeObjectSizeMap;    // post-rename; runtime objects including shadow objects, exceptions and API objects

    public final Map<String, List<String>> shadowClassSlashNameMethodDescriptorMap;
    // The full class hierarchy; we only ever give away deep copies of this object!
    private ClassHierarchy classHierarchy;

    private NodeEnvironment() {
        Map<String, byte[]> generatedShadowJDK = CommonGenerators.generateShadowJDK();
        this.sharedClassLoader = new AvmSharedClassLoader(generatedShadowJDK);
        try {

            this.shadowApiClasses = new Class<?>[] {
                Address.class,
                Blockchain.class,
                Result.class,
                AltBn128.class,
            };

            Class<?>[] arraywrapperClasses = new Class<?>[]{
                    a.IArray.class
                    , a.Array.class
                    , a.ArrayElement.class
                    , a.BooleanArray.class
                    , a.ByteArray.class
                    , a.CharArray.class
                    , a.DoubleArray.class
                    , a.FloatArray.class
                    , a.IntArray.class
                    , a.LongArray.class
                    , a.ObjectArray.class
                    , a.ShortArray.class
            };

            Class<?>[] exceptionwrapperClasses = new Class<?>[]{
                    e.s.java.lang.Throwable.class
            };

            this.shadowClasses = new Class<?>[] {
                    s.java.lang.AssertionError.class
                    , s.java.lang.Boolean.class
                    , s.java.lang.Byte.class
                    , s.java.lang.Character.class
                    , s.java.lang.CharSequence.class
                    , s.java.lang.Class.class
                    , s.java.lang.Comparable.class
                    , s.java.lang.Double.class
                    , s.java.lang.Enum.class
                    , s.java.lang.EnumConstantNotPresentException.class
                    , s.java.lang.Error.class
                    , s.java.lang.Exception.class
                    , s.java.lang.Float.class
                    , s.java.lang.Integer.class
                    , s.java.lang.Iterable.class
                    , s.java.lang.Long.class
                    , s.java.lang.Number.class
                    , s.java.lang.Object.class
                    , s.java.lang.Runnable.class
                    , s.java.lang.RuntimeException.class
                    , s.java.lang.Short.class
                    , s.java.lang.StrictMath.class
                    , s.java.lang.String.class
                    , s.java.lang.StringBuffer.class
                    , s.java.lang.StringBuilder.class
                    , s.java.lang.System.class
                    , s.java.lang.Throwable.class
                    , s.java.lang.TypeNotPresentException.class
                    , s.java.lang.Appendable.class
                    , s.java.lang.Cloneable.class

                    , s.java.lang.invoke.LambdaMetafactory.class
                    , s.java.lang.invoke.StringConcatFactory.class

                    , s.java.lang.Void.class

                    , s.java.math.BigDecimal.class
                    , s.java.math.BigInteger.class
                    , s.java.math.MathContext.class
                    , s.java.math.RoundingMode.class

                    , s.java.util.Arrays.class
                    , s.java.util.Collection.class
                    , s.java.util.Iterator.class
                    , s.java.util.ListIterator.class
                    , s.java.util.Map.class
                    , s.java.util.Map.Entry.class
                    , s.java.util.NoSuchElementException.class
                    , s.java.util.Set.class
                    , s.java.util.List.class
                    , s.java.util.function.Function.class

                    , s.java.util.concurrent.TimeUnit.class
                
                    , s.java.io.Serializable.class
            };

            this.jclClassNames = new HashSet<>();

            // include the shadow classes we implement
            this.jclClassNames.addAll(loadShadowClasses(NodeEnvironment.class.getClassLoader(), shadowClasses));

            // we have to add the common generated exception/error classes as it's not pre-loaded
            this.jclClassNames.addAll(Stream.of(CommonGenerators.kExceptionClassNames)
                    .map(Utilities::fulllyQualifiedNameToInternalName)
                    .collect(Collectors.toList()));

            // include the invoke classes
            this.jclClassNames.add("java/lang/invoke/MethodHandles");
            this.jclClassNames.add("java/lang/invoke/MethodHandle");
            this.jclClassNames.add("java/lang/invoke/MethodType");
            this.jclClassNames.add("java/lang/invoke/CallSite");
            this.jclClassNames.add("java/lang/invoke/MethodHandles$Lookup");

            // Finish the initialization of shared class loader

            // Inject pre generated wrapper class into shared classloader enable more optimization opportunities for us
            this.sharedClassLoader.putIntoDynamicCache(arraywrapperClasses);

            // Inject shadow and api class into shared classloader so we can build a static cache
            this.sharedClassLoader.putIntoStaticCache(this.shadowClasses);

            // Load the AltBn128 class (call the clinit)
            Class<?> instance = Class.forName(AltBn128.class.getName(), true, this.sharedClassLoader);
            RuntimeAssertionError.assertTrue(AltBn128.class == instance);

            this.sharedClassLoader.putIntoStaticCache(this.shadowApiClasses);
            this.sharedClassLoader.putIntoStaticCache(exceptionwrapperClasses);
            this.sharedClassLoader.finishInitialization();

        } catch (ClassNotFoundException e) {
            // This would be a fatal startup error.
            throw RuntimeAssertionError.unexpected(e);
        }

        // Create the constant map.
        this.constantMap = Collections.unmodifiableMap(ConstantsHolder.getConstants());
        RuntimeAssertionError.assertTrue(this.constantMap.size() == 34);

        // create the object size look-up maps
        Map<String, Integer> rtObjectSizeMap = computeRuntimeObjectSizes();
        // This is to ensure the JCLAndAPIHeapInstanceSize is updated with the correct instance size of a newly added JCL or API class
        RuntimeAssertionError.assertTrue(rtObjectSizeMap.size() == 96);

        Map<String, Integer> shadowObjectSizeMap = new HashMap<>(); // pre-rename; shadow objects and exceptions
        Map<String, Integer> apiObjectSizeMap = new HashMap<>(); // post-rename; API objects

        Map<String, Integer> preRenameObjectSizes = new HashMap<>();
        Map<String, Integer> postRenameObjectSizes = new HashMap<>();
        rtObjectSizeMap.forEach((k, v) -> {
            // the shadowed object sizes; and change the class name to the non-shadowed version
            if (k.startsWith(PackageConstants.kShadowSlashPrefix)) {
                shadowObjectSizeMap.put(k.substring(PackageConstants.kShadowSlashPrefix.length()), v);
                postRenameObjectSizes.put(k, v);
            }
            // the object size of API classes
            if (k.startsWith(PackageConstants.kShadowApiSlashPrefix)) {
                apiObjectSizeMap.put(k, v);
                preRenameObjectSizes.put(k.substring(PackageConstants.kShadowApiSlashPrefix.length()), v);
            }
        });
        preRenameObjectSizes.putAll(shadowObjectSizeMap);
        postRenameObjectSizes.putAll(apiObjectSizeMap);

        this.preRenameRuntimeObjectSizeMap = Collections.unmodifiableMap(preRenameObjectSizes);
        this.postRenameRuntimeObjectSizeMap = Collections.unmodifiableMap(postRenameObjectSizes);

        this.shadowClassSlashNameMethodDescriptorMap = Collections.unmodifiableMap(getShadowClassSlashNameMethodDescriptorMap());
        this.classHierarchy = buildJCLAndAPIClassHierarchy();
    }

    // This is an example of the more "factory-like" nature of the NodeEnvironment.
    public AvmClassLoader createInvocationClassLoader(Map<String, byte[]> finalContractClasses) {
        return new AvmClassLoader(this.sharedClassLoader, finalContractClasses);
    }

    public Class<?> loadSharedClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, this.sharedClassLoader);
    }

    /**
     * This method only exists for unit tests.  Returns true if clazz was loaded by the shared loader.
     */
    public boolean isClassFromSharedLoader(Class<?> clazz) {
        return (this.sharedClassLoader == clazz.getClassLoader());
    }

    /**
     * Returns whether the class is from our custom JCL.
     *
     * @param classNameSlash
     * @return
     */
    public boolean isClassFromJCL(String classNameSlash) {
        return this.jclClassNames.contains(classNameSlash);
    }

    public List<String> getJclSlashClassNames() {
        List<String> jclClassNamesCopy = new ArrayList<>(this.jclClassNames);
        return jclClassNamesCopy;
    }

    /**
     * @return The map of constants (specified constant identity hash codes to constant instances).
     */
    public Map<Integer, s.java.lang.Object> getConstantMap() {
        return this.constantMap;
    }

    /**
     * Creates a new long-lived AVM instance.  The intention is that only one AVM instance will be created and reused for each transaction.
     * NOTE:  This is only in the NodeEnvironment since it is a long-lived singleton but this method has no strong connection to it so it
     * could be moved in the future.
     *
     * @param instrumentationFactory The factory to build IInstrumentation instances for the AVM's threads.
     * @param capabilities The external capabilities which this AVM instance can use.
     * @param configuration The configuration options for this new AVM instance.
     * @return The long-lived AVM instance.
     */
    public AvmImpl buildAvmInstance(IInstrumentationFactory instrumentationFactory, IExternalCapabilities capabilities, AvmConfiguration configuration) {
        AvmImpl avm = new AvmImpl(instrumentationFactory, capabilities, configuration);
        avm.start();
        return avm;
    }

    private static Set<String> loadShadowClasses(ClassLoader loader, Class<?>[] shadowClasses) throws ClassNotFoundException {
        // Create the fake IInstrumentation.
        IInstrumentation instrumentation = new IInstrumentation() {
            @Override
            public void chargeEnergy(int cost) throws OutOfEnergyException {
                // Shadow enum class will create array wrapper with <clinit>
                // Ignore the charge energy request in this case
            }
            @Override
            public long energyLeft() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public <T> s.java.lang.Class<T> wrapAsClass(Class<T> input) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public int getNextHashCodeAndIncrement() {
                // Only constants should end up being allocated under this so set them to the constant hash code we will over-write with their
                // specification values, after.
                return Integer.MIN_VALUE;
            }
            @Override
            public void bootstrapOnly() {
                // This is ok since we are the bootstrapping helper.
            }
            @Override
            public void setAbortState() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void clearAbortState() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public s.java.lang.String wrapAsString(String input) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public s.java.lang.Object unwrapThrowable(Throwable t) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public Throwable wrapAsThrowable(s.java.lang.Object arg) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public int getCurStackSize() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public int getCurStackDepth() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void enterMethod(int frameSize) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void exitMethod(int frameSize) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void enterCatchBlock(int depth, int size) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public int peekNextHashCode() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void forceNextHashCode(int nextHashCode) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void enterNewFrame(ClassLoader contractLoader, long energyLeft, int nextHashCode, InternedClasses classWrappers) {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public void exitCurrentFrame() {
                throw RuntimeAssertionError.unreachable("Nobody should be calling this");
            }
            @Override
            public boolean isLoadedByCurrentClassLoader(java.lang.Class userClass) {
                throw RuntimeAssertionError.unreachable("Not expected here.");
            }
        };

        // Load all the classes - even just mentioning these might cause them to be loaded, even before the Class.forName().
        InstrumentationHelpers.attachThread(instrumentation);
        Set<String> loadedClassNames = loadAndInitializeClasses(loader, shadowClasses);
        InstrumentationHelpers.detachThread(instrumentation);

        return loadedClassNames;
    }

    private static Set<String> loadAndInitializeClasses(ClassLoader loader, Class<?>... classes) throws ClassNotFoundException {
        Set<String> classNames = new HashSet<>();

        // (note that the loader.loadClass() doesn't invoke <clinit> so we use Class.forName() - this "initialize" flag should do that).
        boolean initialize = true;
        for (Class<?> clazz : classes) {
            Class<?> instance = Class.forName(clazz.getName(), initialize, loader);
            RuntimeAssertionError.assertTrue(clazz == instance);

            String className = Utilities.fulllyQualifiedNameToInternalName(clazz.getName());
            classNames.add(className.substring(PackageConstants.kShadowSlashPrefix.length()));
        }

        return classNames;
    }




    /**
     * Returns a deep copy of a class hierarchy that already is populated with all of the shadow
     * JCL and API classes.
     */
    public ClassHierarchy deepCopyOfClassHierarchy() {
        RuntimeAssertionError.assertTrue(this.classHierarchy != null);
        return this.classHierarchy.deepCopy();
    }

    /**
     * Computes the object size of shadow java.base classes
     *
     * @return a mapping between class name and object size
     * <p>
     * Class name is in the JVM internal name format, see {@link org.aion.avm.core.util.Helpers#fulllyQualifiedNameToInternalName(String)}
     */
    private Map<String, Integer> computeRuntimeObjectSizes() {
        List<String> classNames = new ArrayList<>();
        classNames.addAll(Arrays.stream(this.shadowApiClasses).map(c -> Utilities.fulllyQualifiedNameToInternalName(c.getName())).collect(Collectors.toList()));
        classNames.addAll(Arrays.stream(this.shadowClasses).map(c -> Utilities.fulllyQualifiedNameToInternalName(c.getName())).collect(Collectors.toList()));

        Map<String, Integer> objectHeapSizeMap = new HashMap<>();
        for(String name: classNames){
            objectHeapSizeMap.put(name, JCLAndAPIHeapInstanceSize.getAllocationSizeForJCLAndAPISlashClass(name));
        }

        // add the generated classes, i.e., exceptions in the generated shadow JDK
        Stream.of(CommonGenerators.kExceptionClassNames)
                .filter(s -> !CommonGenerators.kHandWrittenExceptionClassNames.contains(s))
                .map(name -> Utilities.fulllyQualifiedNameToInternalName(PackageConstants.kShadowDotPrefix + name))
                .forEach(s -> objectHeapSizeMap.put(s, JCLAndAPIHeapInstanceSize.getAllocationSizeForGeneratedExceptionSlashClass()));
        return objectHeapSizeMap;
    }

    private ClassHierarchy buildJCLAndAPIClassHierarchy() {
        Map<String, byte[]> classBytesByQualifiedNames = new HashMap<>();
        String mainClassName = "java.lang.Object";

        List<Class<?>> classes = new ArrayList<>();
        classes.addAll(Arrays.asList(this.shadowApiClasses));
        classes.addAll(Arrays.asList(this.shadowClasses));
        for (Class<?> clazz : classes) {
            try {
                String name = clazz.getName();
                InputStream bytecode = clazz.getClassLoader().getResourceAsStream(name.replaceAll("\\.", "/") + ".class");
                classBytesByQualifiedNames.put(name, bytecode.readAllBytes());
            } catch (IOException e) {
                RuntimeAssertionError.unexpected(e);
            }
        }
        LoadedJar runtimeJar = new LoadedJar(classBytesByQualifiedNames, mainClassName);

        // Construct the full class hierarchy.
        ClassInformationFactory classInfoFactory = new ClassInformationFactory();
        Set<ClassInformation> classInfos = classInfoFactory.fromPostRenameJar(runtimeJar);

        return new ClassHierarchyBuilder()
                .addPostRenameNonUserDefinedClasses(classInfos)
                .build();
    }

    private Map<String, List<String>> getShadowClassSlashNameMethodDescriptorMap(){
        try {
            return MethodDescriptorCollector.getClassNameMethodDescriptorMap(getJclSlashClassNames(), this.sharedClassLoader);
        } catch (ClassNotFoundException e) {
            throw RuntimeAssertionError.unexpected(e);
        }
    }

}
