package avm;

import java.math.BigInteger;

/**
 * Implementation of functions required to verify a SNARK
 * <p>
 * This pre-compile contract implements EC addition (in G1), scalar multiplication (in G1) and pairing check functions
 * over the alt_bn128 curve.
 * <p>
 * We use the alt_bn128 curve with parameters:
 * p = 21888242871839275222246405745257275088696311157297823662689037894645226208583
 * q = 21888242871839275222246405745257275088548364400416034343698204186575808495617
 * <p>
 * Note that the interface to this class is a bunch of byte arrays, since the implementation
 * goes ahead and calls the alt_bn128 native library
 */
@SuppressWarnings("unused")
public class AltBn128 {

    /**
     * Computes EC addition in G1
     * <br/>
     *
     * Failure Mode: Any illegal points yield a '0' as result.
     * <br/>
     *
     * @param point1 point in G1, encoded like so: [p.x || p.y]. Each coordinate is byte aligned to 32 bytes.
     * @param point2 point in G1, encoded like so: [p.x || p.y]. Each coordinate is byte aligned to 32 bytes.
     *
     * @throws IllegalArgumentException if the input arguments fail validation
     * @throws RuntimeException if error occurs in native library (e.g. result of calculation yields a point not on curve)
     */
    public static byte[] g1EcAdd(byte[] point1, byte[] point2) throws IllegalArgumentException {
        return null;
    }

    /**
     * Computes scalar multiplication in G1
     * <br/>
     *
     * Failure Mode: Any illegal points as input yields an RuntimeException with message "NotOnCurve".
     * <br/>
     *
     * @param point point in G1, encoded like so: [p.x || p.y]. Each coordinate is byte aligned to 32 bytes.
     * @param scalar natural number (> 0), byte aligned to 32 bytes.
     *
     * @throws IllegalArgumentException if the input arguments fail validation
     * @throws RuntimeException if error occurs in native library (e.g. result of calculation yields a point not on curve)
     */
    public static byte[] g1EcMul(byte[] point, BigInteger scalar) throws IllegalArgumentException {
        return null;
    }

    /**
     * The Pairing itself is a transformation of the form G1 x G2 -> Gt, <br/>
     * where Gt is a subgroup of roots of unity in Fp12 field<br/>
     * <br/>
     *
     * Pairing Check input is a sequence of point pairs, the result is either success (true) or failure (false) <br/>
     * <br/>
     *
     * Failure Mode: Any illegal points as input yield a result 'false'.
     * <br/>
     *
     * @param g1_point_list list of points in G1, encoded like so: [p1.x || p1.y || p2.x || p2.y || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     * @param g2_point_list list of points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     *
     * @throws IllegalArgumentException if the input arguments fail validation
     * @throws RuntimeException if error occurs in native library (e.g. result of calculation yields a point not on curve)
     */
    public static boolean isPairingProdEqualToOne(byte[] g1_point_list, byte[] g2_point_list) throws IllegalArgumentException {
        return false;
    }

}
