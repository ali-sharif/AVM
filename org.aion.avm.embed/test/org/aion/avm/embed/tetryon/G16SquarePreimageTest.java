package org.aion.avm.embed.tetryon;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.embed.tetryon.bn128.*;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.types.TransactionStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigInteger;

public class G16SquarePreimageTest {

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private static Address sender = avmRule.getPreminedAccount();
    private static Address contract;

    @BeforeClass
    public static void deployDapp() {
        byte[] g16DappBytes = avmRule.getDappBytes(G16SquarePreimage.class, null, 1,
                Fp.class, Fp2.class, G1.class, G1Point.class, G2.class, G2Point.class, Pairing.class, Util.class);
        contract = avmRule.deploy(sender, BigInteger.ZERO, g16DappBytes).getDappAddress();
    }

    // positive test-case for square pre-image verifier: a=337, b=113569 (a^2 == b)
    @Test
    public void g16TestVerify() {
        G1Point a = new G1Point(
                new Fp(new BigInteger("07f4a1ab12b1211149fa0aed8ade3442b774893dcd1caffb8693ade54999c164", 16)),
                new Fp(new BigInteger("23b7f10c5e1aeaffafa088f1412c0f307969ba3f8f9d5920214a4cb91693fab5", 16)));

        G2Point b = new G2Point(
                new Fp2(new BigInteger("1f6cc814cf1df1ceb663378c496f168bcd21e19bb529e90fcf3721f8df6b4128", 16),
                        new BigInteger("079ee30e2c79e15be67645838a3177f681ab111edacf6f4867e8eed753ed9681", 16)),
                new Fp2(new BigInteger("2779dd0accaa1391e29ad54bf065819cac3129edda4eaf909d6ea2c7495a47f7", 16),
                        new BigInteger("20105b11ae5fbdc7067102d4260c8913cdcb512632680221d7644f9928a7e51d", 16)));

        G1Point c = new G1Point(
                new Fp(new BigInteger("153c3a313679a5c11010c3339ff4f787246ed2e8d736efb615aeb321f5a22432", 16)),
                new Fp(new BigInteger("06691d8441c35768a4ca87a5f5ee7d721bf13115d2a16726c12cda295a19bf09", 16)));


        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba1", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input, new G16SquarePreimage.Proof(a, b, c).serialize());
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        // transaction should succeed
        Assert.assertTrue(r.getReceiptStatus().isSuccess());
        // verify should return "true"
        Assert.assertTrue(new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());
    }

    // negative test-case for square pre-image verifier: a=337, b=113570 (a^2 != b)
    @Test
    public void g16TestReject() {
        G1Point a = new G1Point(
                new Fp(new BigInteger("1946d8503f2bddd05511bfcebc502a620055b4c3d2c3c104e5c473d15b789a80", 16)),
                new Fp(new BigInteger("1d1f8fffa65efb700e695f25f2a932385c144e7e964c85b9d74cca78672834ec", 16)));

        G2Point b = new G2Point(
                new Fp2(new BigInteger("2354063529fbbd0688744273b329b6b3d6a6f9d7a837dccb1617cb0e52d72609", 16),
                        new BigInteger("1af73fa504d700c6e2f7730940a0139319264a6463c4303878aab72cf0e5e2b2", 16)),
                new Fp2(new BigInteger("08645e9bbd3baef396dce9efcc844d79c191a2f6c1ab1c87fb3859c76da9ee43", 16),
                        new BigInteger("0c48749defa64ff75dd86b4cf4efaa5a4c45ed17ca25efb8ea9b183eccee1303", 16)));

        G1Point c = new G1Point(
                new Fp(new BigInteger("15ee5aef1ee660c3e4abecf8c31960b4ae106918ae8d403138607413a4d75f38", 16)),
                new Fp(new BigInteger("066d8491786dbf2d5e45a4006a7252333c8ffb083e3b60d00c4c9044ae9a5760", 16)));


        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba2", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input, new G16SquarePreimage.Proof(a, b, c).serialize());
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        // transaction should succeed
        Assert.assertTrue(r.getReceiptStatus().isSuccess());
        // verify should return "false"
        Assert.assertFalse(new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());
    }

    @Test
    public void g16TestBadInput1() {
        //
        BigInteger[] input = new BigInteger[]{
                new BigInteger("000000000000000000000000000000000000000000000000000000000001bba2", 16),
                new BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16)};

        byte[] txData = ABIUtil.encodeMethodArguments("verify", input, new byte[4]);
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        TransactionStatus s = r.getReceiptStatus();
        Assert.assertTrue(s.isReverted());
    }

    @Test
    public void g16TestBadInput2() {
        G1Point a = new G1Point(
                new Fp(new BigInteger("07f4a1ab12b1211149fa0aed8ade3442b774893dcd1caffb8693ade54999c164", 16)),
                new Fp(new BigInteger("23b7f10c5e1aeaffafa088f1412c0f307969ba3f8f9d5920214a4cb91693fab5", 16)));

        G2Point b = new G2Point(
                new Fp2(new BigInteger("1f6cc814cf1df1ceb663378c496f168bcd21e19bb529e90fcf3721f8df6b4128", 16),
                        new BigInteger("079ee30e2c79e15be67645838a3177f681ab111edacf6f4867e8eed753ed9681", 16)),
                new Fp2(new BigInteger("2779dd0accaa1391e29ad54bf065819cac3129edda4eaf909d6ea2c7495a47f7", 16),
                        new BigInteger("20105b11ae5fbdc7067102d4260c8913cdcb512632680221d7644f9928a7e51d", 16)));

        G1Point c = new G1Point(
                new Fp(new BigInteger("153c3a313679a5c11010c3339ff4f787246ed2e8d736efb615aeb321f5a22432", 16)),
                new Fp(new BigInteger("06691d8441c35768a4ca87a5f5ee7d721bf13115d2a16726c12cda295a19bf09", 16)));

        byte[] txData = ABIUtil.encodeMethodArguments("verify", new BigInteger[]{}, new G16SquarePreimage.Proof(a, b, c).serialize());
        AvmRule.ResultWrapper r = avmRule.call(sender, contract, BigInteger.ZERO, txData);

        TransactionStatus s = r.getReceiptStatus();
        Assert.assertTrue(s.isReverted());
    }
}
