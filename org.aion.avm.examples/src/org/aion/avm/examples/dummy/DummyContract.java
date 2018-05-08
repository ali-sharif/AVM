package org.aion.avm.examples.dummy;

import org.aion.avm.base.Contract;
import org.aion.avm.rt.BlockchainRuntime;

public class DummyContract extends Contract {

    @Override
    public byte[] run(byte[] input, BlockchainRuntime rt) {
        C1 c = new C1();
        c.doSomething();

        return null;
    }
}
