package org.aion.avm.core;

import java.math.BigInteger;

import org.aion.types.AionAddress;
import org.aion.types.InternalTransaction;
import org.aion.types.Transaction;
import org.aion.avm.core.blockchainruntime.EmptyCapabilities;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.userlib.CodeAndArguments;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIEncoder;
import org.aion.avm.userlib.abi.ABIException;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;

import org.aion.kernel.TestingBlock;
import org.aion.kernel.TestingState;
import org.aion.types.TransactionResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class MetaTransactionTest {
    private static AionAddress DEPLOYER = TestingState.PREMINED_ADDRESS;
    private static TestingBlock BLOCK;
    private static TestingState KERNEL;
    private static EmptyCapabilities CAPABILITIES;
    private static AvmImpl AVM;

    @BeforeClass
    public static void setupClass() {
        BLOCK = new TestingBlock(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        KERNEL = new TestingState(BLOCK);
        
        CAPABILITIES = new EmptyCapabilities();
        AvmConfiguration config = new AvmConfiguration();
        AVM = CommonAvmFactory.buildAvmInstanceForConfiguration(CAPABILITIES, config);
    }

    @AfterClass
    public static void tearDownClass() {
        AVM.shutdown();
    }

    @Test
    public void testInlineBalanceTransfer() {
        // Deploy initial contract.
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);

        // Create a transaction to make a basic balance transfer.
        AionAddress targetAddress = Helpers.randomAddress();
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        byte[] serializedTransaction = buildInnerMetaTransactionFromDeployer(targetAddress, valueToSend, 1L, contractAddress, new byte[0]);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(targetAddress));
        
        // Send the transaction as an inline meta-transaction.
        byte[] callData = encodeCallByteArray("callInline", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        // Balance transfer returns null.
        Assert.assertNull(new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
        
        // Verify final state.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(targetAddress));
    }

    @Test
    public void testInlineContractCall() {
        // Deploy initial contract.
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);

        // Create a transaction to call back into the contract, itself, as just the identity invocation (returns what it is given).
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        byte[] inputData = new byte[] {1,2,3,4,5};
        byte[] invokeArguments = new ABIStreamingEncoder().encodeOneString("identity").encodeOneByteArray(inputData).toBytes();
        byte[] serializedTransaction = buildInnerMetaTransactionFromDeployer(contractAddress, valueToSend, 1L, contractAddress, invokeArguments);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(contractAddress));
        
        // Send the transaction as an inline meta-transaction.
        byte[] callData = encodeCallByteArray("callInline", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        // Identity returns the data it is given.
        Assert.assertArrayEquals(inputData, new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
        
        // Verify final state.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(contractAddress));
    }

    @Test
    public void testStoreBalanceTransferDoubleSend() {
        // Deploy initial contract.
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);

        // Create a transaction to make a basic balance transfer.
        AionAddress targetAddress = Helpers.randomAddress();
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        byte[] serializedTransaction = buildInnerMetaTransactionFromDeployer(targetAddress, valueToSend, 2L, contractAddress, new byte[0]);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(targetAddress));
        
        // Store this on-chain.
        byte[] callData = encodeCallByteArray("store", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        
        // Verify that it hasn't yet run.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(targetAddress));
        
        // Invoke the on-chain transaction.
        callData = encodeCall("call");
        tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        
        // Observe the balance changed.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(targetAddress));
        
        // Invoke the on-chain transaction, again - this should cause a nonce failure which we aren't handline, so it is an exception.
        callData = encodeCall("call");
        tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isFailed());
        
        // Verify that the balance didn't change again.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(targetAddress));
    }

    @Test
    public void testInlineContractDeploy() {
        // Deploy initial contract.
        // (we assemble this manually to keep it small since we don't have the optimizer in this project).
        byte[] jar = JarBuilder.buildJarForMainAndClasses(MetaTransactionTarget.class, ABIEncoder.class, ABIDecoder.class, ABIException.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);

        // Create a transaction to call back into the contract, itself, as just the identity invocation (returns what it is given).
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        byte[] serializedTransaction = buildInnerMetaTransactionFromDeployer(null, valueToSend, 1L, contractAddress, codeAndArgs);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(contractAddress));
        
        // Send the transaction as an inline meta-transaction.
        byte[] callData = encodeCallByteArray("createInline", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        
        // The result should be an encoded contract address so check that it is there and has the money.
        AionAddress newContract = new AionAddress(new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
        Assert.assertEquals(valueToSend, KERNEL.getBalance(newContract));
        Assert.assertArrayEquals(jar, KERNEL.getCode(newContract));
        
        // Now, send a simple transaction to make sure we can run this and the returned address was meaningful.
        byte[] inputData = new byte[] {1,2,3,4,5};
        callData = encodeCallByteArray("identity", inputData);
        tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        Assert.assertArrayEquals(inputData, new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
    }

    @Test
    public void testInlineContractCallDepth() {
        BigInteger initialNonce = KERNEL.getNonce(DEPLOYER);
        // Deploy initial contract.
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);
        
        // We will try sending a simple transaction, nested 9 times, since that should be the limit.
        // We will eventually call "identity", nested in 8 levels of "callInline".
        // These will be called backward so we need to attach nonces in reverse order.
        long intermediateLevels = 8L;
        long firstInnerNonceBias = 1L;
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        byte[] inputData = new byte[] {1,2,3,4,5};
        byte[] invokeArguments = new ABIStreamingEncoder().encodeOneString("identity").encodeOneByteArray(inputData).toBytes();
        byte[] terminalTransaction = buildInnerMetaTransactionFromDeployer(contractAddress, valueToSend, intermediateLevels + 1, contractAddress, invokeArguments);
        byte[] serializedTransaction = recursiveEncode(contractAddress, valueToSend, firstInnerNonceBias, intermediateLevels, 0L, contractAddress, terminalTransaction);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(contractAddress));
        
        // Send the transaction as an inline meta-transaction.
        byte[] callData = encodeCallByteArray("callInline", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        // Identity returns the data it is given.
        Assert.assertArrayEquals(inputData, new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
        
        // Verify final state.
        BigInteger baselineBalance = valueToSend.multiply(BigInteger.valueOf(9L));
        Assert.assertEquals(baselineBalance, KERNEL.getBalance(contractAddress));
        // Also verify the nonce reflects these 11 calls:  1 deployment, 1 external, and 9 internal meta.
        Assert.assertEquals(initialNonce.add(BigInteger.valueOf(11L)), KERNEL.getNonce(DEPLOYER));
        Assert.assertEquals(9, result.internalTransactions.size());
        
        // Now, send one which should fail (10 iterations - 9 intermediate and 1 final).
        // NOTE:  only the final transaction will fail - the other 8 internal transactions will pass
        intermediateLevels = 9L;
        invokeArguments = new ABIStreamingEncoder().encodeOneString("identity").encodeOneByteArray(inputData).toBytes();
        terminalTransaction = buildInnerMetaTransactionFromDeployer(contractAddress, valueToSend, intermediateLevels + 1, contractAddress, invokeArguments);
        serializedTransaction = recursiveEncode(contractAddress, valueToSend, firstInnerNonceBias, intermediateLevels, 0L, contractAddress, terminalTransaction);
        
        // Send the transaction as an inline meta-transaction.
        callData = encodeCallByteArray("callInline", serializedTransaction);
        tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        
        // Verify final state is only updated to account for the 8 successful frames.
        // NOTE:  This failure behaviour requires some explanation:  an invocation stack depth overflow causes fatal error within the caller.
        // This means that 9 calls are ok but attempting 10 will only see 8 of them complete.
        Assert.assertEquals(valueToSend.multiply(BigInteger.valueOf(8L)).add(baselineBalance), KERNEL.getBalance(contractAddress));
        // Also verify the final nonce only reflects the successfully executed calls:
        // 1 deployment
        // 1 + 9 for original successful sequences.
        // 1 + 9 successes in this sequence (excluding the failure).
        Assert.assertEquals(initialNonce.add(BigInteger.valueOf(21L)), KERNEL.getNonce(DEPLOYER));
        Assert.assertEquals(9, result.internalTransactions.size());
    }

    @Test
    public void testOrigin() {
        // Deploy initial contract.
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        AionAddress contractAddress = createDApp(codeAndArgs);
        
        // Create the new user account which will sign but not pay for the transaction.
        AionAddress freeloaderAddress = Helpers.randomAddress();
        
        // Create the inner transaction which will verify the observed origin.
        BigInteger valueToSend = BigInteger.ZERO;
        byte[] invokeArguments = new ABIStreamingEncoder().encodeOneString("checkOrigin").toBytes();
        byte[] serializedTransaction = buildInnerMetaTransaction(freeloaderAddress, contractAddress, valueToSend, BigInteger.ZERO, contractAddress, invokeArguments);
        
        // Verify initial state.
        Assert.assertEquals(BigInteger.ZERO, KERNEL.getBalance(contractAddress));
        
        // Send the transaction as an inline meta-transaction.
        byte[] callData = encodeCallByteArray("callInline", serializedTransaction);
        Transaction tx = AvmTransactionUtil.call(DEPLOYER, contractAddress, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, callData, 20_000_000l, 1L);
        TransactionResult result = AVM.run(KERNEL, new Transaction[] {tx}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
        Assert.assertTrue(result.transactionStatus.isSuccess());
        // Identity returns the data it is given.
        Assert.assertArrayEquals(freeloaderAddress.toByteArray(), new ABIDecoder(result.copyOfTransactionOutput().get()).decodeOneByteArray());
        // Verify that we see this meta-transaction in the results.
        int normal = 0;
        int meta = 0;
        for (InternalTransaction internal : result.internalTransactions) {
            if (null != internal.copyOfInvokableHash()) {
                meta += 1;
            } else {
                normal += 1;
            }
        }
        Assert.assertEquals(0, normal);
        Assert.assertEquals(1, meta);
        
        // Verify final state.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(contractAddress));
    }

    @Test
    public void testInlineTransferOnDeploy() {
        // Get the nonce for the deployment.
        BigInteger deploymentNonce = KERNEL.getNonce(DEPLOYER);
        // ... and the nonce for the transaction within it.
        BigInteger innerTransactionNonce = deploymentNonce.add(BigInteger.ONE);
        
        // Create the target address we will fund.
        AionAddress targetAddress = Helpers.randomAddress();
        // We also need to know the executor address which will be the address where the contract is deployed
        // (we just pass a fake transaction with the correct sender and nonce into the helper).
        AionAddress executorAddress = CAPABILITIES.generateContractAddress(DEPLOYER, deploymentNonce);
        // Create the value we wish to send which we can use to verify the balance, later.
        BigInteger valueToSend = BigInteger.valueOf(1_000_000_000L);
        
        // Prepare the inner transaction.
        byte[] innerTransaction = buildInnerMetaTransaction(DEPLOYER, targetAddress, valueToSend, innerTransactionNonce, executorAddress, new byte[0]);
        // Package it with the deployment (we don't need to wrap this in ABI).
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(MetaTransactionTarget.class);
        byte[] codeAndArgs = new CodeAndArguments(jar, innerTransaction).encodeToBytes();
        
        // Deploy the contract and verify that the address was as expected.
        AionAddress contractAddress = createDApp(codeAndArgs);
        Assert.assertEquals(executorAddress, contractAddress);
        
        // Verify that the balance transfer happened and that the deployer's nonce incremented.
        Assert.assertEquals(valueToSend, KERNEL.getBalance(targetAddress));
        Assert.assertEquals(innerTransactionNonce.add(BigInteger.ONE), KERNEL.getNonce(DEPLOYER));
    }


    private byte[] recursiveEncode(AionAddress targetAddress, BigInteger valueToSend, long baseNonce, long iterations, long currentIteration, AionAddress executor, byte[] data) {
        byte[] result = null;
        // This works like a for look so terminate when they match.
        if (iterations == currentIteration) {
            result = data;
        } else {
            byte[] downStream = recursiveEncode(targetAddress, valueToSend, baseNonce, iterations, currentIteration + 1, executor, data);
            long nonceBias = baseNonce + currentIteration;
            valueToSend = BigInteger.valueOf(1_000_000_000L);
            byte[] invokeArguments = new ABIStreamingEncoder().encodeOneString("callInline").encodeOneByteArray(downStream).toBytes();
            result = buildInnerMetaTransactionFromDeployer(targetAddress, valueToSend, nonceBias, targetAddress, invokeArguments);
        }
        return result;
    }

    // NOTE:  If targetAddress is null, this is a create.
    private byte[] buildInnerMetaTransaction(AionAddress senderAddress, AionAddress targetAddress, BigInteger valueToSend, BigInteger nonce, AionAddress executor, byte[] data) {
        TestingMetaEncoder.MetaTransaction transaction = new TestingMetaEncoder.MetaTransaction();
        transaction.senderAddress = senderAddress;
        transaction.targetAddress = targetAddress;
        transaction.value = valueToSend;
        transaction.nonce = nonce;
        transaction.executor = executor;
        transaction.data = data;
        transaction.signature = new byte[] { 0x1 };
        return TestingMetaEncoder.encode(transaction);
    }

    // NOTE:  If targetAddress is null, this is a create.
    private byte[] buildInnerMetaTransactionFromDeployer(AionAddress targetAddress, BigInteger valueToSend, long nonceBias, AionAddress executor, byte[] data) {
        TestingMetaEncoder.MetaTransaction transaction = new TestingMetaEncoder.MetaTransaction();
        transaction.senderAddress = DEPLOYER;
        transaction.targetAddress = targetAddress;
        transaction.value = valueToSend;
        transaction.nonce = KERNEL.getNonce(DEPLOYER).add(BigInteger.valueOf(nonceBias));
        transaction.executor = executor;
        transaction.data = data;
        transaction.signature = new byte[] { 0x1 };
        return TestingMetaEncoder.encode(transaction);
    }

    private AionAddress createDApp(byte[] createData) {
        TransactionResult result1 = createDAppCanFail(createData);
        Assert.assertTrue(result1.transactionStatus.isSuccess());
        return new AionAddress(result1.copyOfTransactionOutput().orElseThrow());
    }

    private TransactionResult createDAppCanFail(byte[] createData) {
        long energyLimit = 10_000_000l;
        long energyPrice = 1l;
        Transaction tx1 = AvmTransactionUtil.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, createData, energyLimit, energyPrice);
        return AVM.run(KERNEL, new Transaction[] {tx1}, ExecutionType.ASSUME_MAINCHAIN, KERNEL.getBlockNumber() - 1)[0].getResult();
    }

    private static byte[] encodeCall(String methodName) {
        return new ABIStreamingEncoder()
                .encodeOneString(methodName)
                .toBytes();
    }

    private static byte[] encodeCallByteArray(String methodName, byte[] arg) {
        return new ABIStreamingEncoder()
                .encodeOneString(methodName)
                .encodeOneByteArray(arg)
                .toBytes();
    }
}