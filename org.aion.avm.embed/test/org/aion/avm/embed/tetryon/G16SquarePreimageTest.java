package org.aion.avm.embed.tetryon;

import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.embed.tetryon.bn128.*;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigInteger;

public class G16SquarePreimageTest {

    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private static Address sender = avmRule.getPreminedAccount();
    private static Address g16Contract;

    @BeforeClass
    public static void deployDapp() {
        byte[] g16DappBytes = avmRule.getDappBytes(G16SquarePreimage.class, null, 1,
                Fp.class, Fp2.class, G1.class, G1Point.class, G2.class, G2Point.class, Pairing.class, Util.class);
        g16Contract = avmRule.deploy(sender, BigInteger.ZERO, g16DappBytes).getDappAddress();
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
        AvmRule.ResultWrapper r = avmRule.call(sender, g16Contract, BigInteger.ZERO, txData);

        // transaction should succeed
        Assert.assertTrue(r.getReceiptStatus().isSuccess());
        // verify should return "true"
        Assert.assertTrue(new ABIDecoder(r.getTransactionResult().copyOfTransactionOutput().orElseThrow()).decodeOneBoolean());
    }

}
