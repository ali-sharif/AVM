package org.aion.tetryon;

/**
 * Don't want to define a custom exception for errors that could occur in the native library.
 */
public class AltBn128Jni {
    public native byte[] g1EcAdd(byte[] point1, byte[] point2) throws RuntimeException;
    public native byte[] g1EcMul(byte[] point, byte[] scalar) throws RuntimeException;
    public native boolean ecPair(byte[] g1_point_list, byte[] g2_point_list) throws RuntimeException;
}
