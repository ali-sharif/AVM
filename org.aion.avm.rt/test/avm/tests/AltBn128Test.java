package avm.tests;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static avm.AltBn128.g1EcAdd;
import static org.junit.Assert.assertEquals;

public class AltBn128Test {

    @Test
    public void g1EcAddTest() {
        byte[] point1 = new byte[32*2];
        byte[] p1x = new BigInteger("222480c9f95409bfa4ac6ae890b9c150bc88542b87b352e92950c340458b0c09",16).toByteArray();
        System.arraycopy(p1x, 0, point1, 0, p1x.length);
        byte[] p1y = new BigInteger("2976efd698cf23b414ea622b3f720dd9080d679042482ff3668cb2e32cad8ae2",16).toByteArray();
        System.arraycopy(p1y, 0, point1, 32, p1y.length);

        byte[] point2 = new byte[32*2];
        byte[] p2x = new BigInteger("1bd20beca3d8d28e536d2b5bd3bf36d76af68af5e6c96ca6e5519ba9ff8f5332",16).toByteArray();
        System.arraycopy(p2x, 0, point2, 0, p2x.length);
        byte[] p2y = new BigInteger("2a53edf6b48bcf5cb1c0b4ad1d36dfce06a79dcd6526f1c386a14d8ce4649844",16).toByteArray();
        System.arraycopy(p2y, 0, point2, 32, p2y.length);

        byte[] result = g1EcAdd(point1, point2);

        byte[] rx = Arrays.copyOfRange(result, 0, 32);
        byte[] ry = Arrays.copyOfRange(result, 32, result.length);

        assertEquals(rx, new BigInteger("16c7c4042e3a725ddbacf197c519c3dcad2bc87dfd9ac7e1e1631154ee0b7d9c", 16).toByteArray());
        assertEquals(ry, new BigInteger("19cd640dd28c9811ebaaa095a16b16190d08d6906c4f926fce581985fe35be0e", 16).toByteArray());
    }
}
