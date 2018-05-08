package org.aion.avm.examples.helloworld;

import org.aion.avm.base.Contract;
import org.aion.avm.rt.BlockchainRuntime;

public class HelloWorld extends Contract {

    public int foo;

    public static int bar;

    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public byte[] run(byte[] input, BlockchainRuntime rt) {
        return "Hello, world!".getBytes();
    }
}
