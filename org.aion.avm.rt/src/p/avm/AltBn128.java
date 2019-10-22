package p.avm;

import a.ByteArray;
import i.IInstrumentation;
import org.aion.avm.RuntimeMethodFeeSchedule;
import org.aion.tetryon.AltBn128Jni;
import s.java.lang.Object;
import s.java.math.BigInteger;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
/**
 * Java wrapper for alt-bn128 curve implemented here: https://github.com/paritytech/bn
 *
 * todo: the three functions have different failure mode. fix for production.
 * todo: move more of the point validation logic into the JNI wrapper. fix for production.
 */
public class AltBn128 extends Object {

    public static int WORD_SIZE = 32;
    // points in G1 are encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
    public static int G1_POINT_SIZE = 2 * WORD_SIZE;
    // points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x]. Each coordinate is 32-byte aligned.
    public static int G2_POINT_SIZE = 4 * WORD_SIZE;

    // load native libraries
    static {
        try {
            // the only reason to do this, is to avoid touching the java.library.path
            // assumes a file called "libbn_jni.so" lives at the same location as the jar
            System.load(new File("native/linux/libbn_jni.so").getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library for altbn-128");
        }
    }

    @SuppressWarnings("unused")
    private static final class Holder {
        public static final AltBn128Jni INSTANCE = new AltBn128Jni();
    }

    // non-instantiable class
    private AltBn128() { }

    // metering constants
    public static final long RT_BN_METHOD_FEE_PAIRING_BASE = 45_000;
    public static final long RT_BN_METHOD_FEE_PAIRING_INSTANCE = 34_000;

    public static final long AltBn128_avm_g1EcAdd = RuntimeMethodFeeSchedule.RT_METHOD_FEE_LEVEL_5; // native benchmark per op ~ 7us (more expensive over jni)
    public static final long AltBn128_avm_g1EcMul = RuntimeMethodFeeSchedule.RT_METHOD_FEE_LEVEL_6; // native benchmark per op ~ 230us (more expensive over jni)

    public static final long AltBn128_avm_pairingCheck_base = RT_BN_METHOD_FEE_PAIRING_BASE; // native benchmark per op ~ 3ms (1 op) - 1.4ms (amortized over 10 ops) (more expensive over jni)
    public static final long AltBn128_avm_pairingCheck_per_pairing = RT_BN_METHOD_FEE_PAIRING_INSTANCE;

    // Runtime-facing implementation
    /**
     * Computes EC addition in G1
     *
     * We do buffer size validation here (not done in JNI wrapper).
     *
     * Failure Mode: Any illegal points yield a '0' as result.
     *
     * @param point1 point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     * @param point2 point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     *
     * @throws RuntimeException if point addition failed (e.g. due to Not On Curve error)
     * @throws IllegalArgumentException if invalid data provided
     *
     * @return a point in G1.
     */
    public static ByteArray avm_g1EcAdd(ByteArray point1, ByteArray point2) throws Exception {
        // assert valid data.
        require(point1 != null && point2 != null &&
                point1.length() == G1_POINT_SIZE && point2.length() == G1_POINT_SIZE,
                "input point serialization invalid");

        // gas costing
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(AltBn128_avm_g1EcAdd);

        // call jni
        return new ByteArray(Holder.INSTANCE.g1EcAdd(point1.getUnderlying(), point2.getUnderlying()));
    }

    /**
     * Computes scalar multiplication in G1
     *
     * We do buffer size validation here (not done in JNI wrapper).
     *
     * Failure Mode: Any illegal points as input yields an Exception with message "NotOnCurve".
     *
     * @param point point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     * @param scalar natural number (> 0), byte aligned to 32 bytes.
     *
     * @throws RuntimeException if scalar multiplication failed (e.g. due to Not On Curve error)
     * @throws IllegalArgumentException if invalid data provided
     *
     * @return a point in G1.
     */
    public static ByteArray avm_g1EcMul(ByteArray point, BigInteger scalar) throws Exception {
        // assert valid data.
        require(point != null && scalar != null &&
                point.length() == G1_POINT_SIZE && scalar.getUnderlying().signum() != -1,
                "point serialization invalid or negative scalar"); // scalar can't be negative (it can be zero or positive)

        byte[] sdata = scalar.getUnderlying().toByteArray();
        require(sdata.length <= WORD_SIZE, "scalar is too large");

        byte[] sdata_aligned = new byte[WORD_SIZE];
        System.arraycopy(sdata, 0, sdata_aligned, WORD_SIZE - sdata.length, sdata.length);

        // gas costing
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(AltBn128_avm_g1EcMul);

        // call jni
        return new ByteArray(Holder.INSTANCE.g1EcMul(point.getUnderlying(), sdata_aligned));
    }

    /**
     * The Pairing itself is a transformation of the form G1 x G2 -> Gt, <br/>
     * where Gt is a subgroup of roots of unity in Fp12 field<br/>
     * <br/>
     *
     * Pairing Check input is a sequence of point pairs, the result is either success (true) or failure (false) <br/>
     * <br/>
     *
     * We do buffer size validation here (not done in JNI wrapper).
     *
     * @param g1_point_list list of points in G1, encoded like so: [p1.x || p1.y || p2.x || p2.y || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     * @param g2_point_list list of points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     *
     * @throws RuntimeException if pairing product failed (e.g. due to Not On Curve error)
     * @throws IllegalArgumentException if invalid data provided
     *
     * @return true if pairing product equals one, else false.
     *
     */
    public static boolean avm_isPairingProdEqualToOne(ByteArray g1_point_list, ByteArray g2_point_list) throws Exception {
        // assert valid data.
        require(g1_point_list != null && g2_point_list != null &&
                g1_point_list.length() % G1_POINT_SIZE == 0 && g2_point_list.length() % G2_POINT_SIZE == 0,
                "data is not well-aligned");
        int g1_list_size = g1_point_list.length() / G1_POINT_SIZE;
        int g2_list_size = g2_point_list.length() / G2_POINT_SIZE;
        require(g1_list_size == g2_list_size, "g1 and g2 lists must contain same number of points");

        // gas costing: the list size tells you how many pairing operations will be performed
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(
                AltBn128_avm_pairingCheck_base + g1_list_size * AltBn128_avm_pairingCheck_per_pairing);

        // call jni
        return Holder.INSTANCE.ecPair(g1_point_list.getUnderlying(), g2_point_list.getUnderlying());
    }

    private static void require(boolean condition, String message) throws IllegalArgumentException{
        if (!condition) throw new IllegalArgumentException(message);
    }
}
