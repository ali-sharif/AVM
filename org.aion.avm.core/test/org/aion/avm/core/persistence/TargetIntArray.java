package org.aion.avm.core.persistence;

import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IObjectDeserializer;
import org.aion.avm.internal.IObjectSerializer;
import org.aion.avm.internal.IPersistenceToken;


public final class TargetIntArray extends TargetRoot {
    public int[] array;
    public TargetIntArray(int size) {
        this.array = new int[size];
    }
    // Temporarily use IDeserializer and IPersistenceToken to reduce the scope of this commit.
    public TargetIntArray(IDeserializer ignore, IPersistenceToken readIndex) {
        super(ignore, readIndex);
    }
    
    public void serializeSelf(Class<?> stopBefore, IObjectSerializer serializer) {
        super.serializeSelf(TargetIntArray.class, serializer);
        serializer.writeInt(this.array.length);
        for (int elt : this.array) {
            serializer.writeInt(elt);
        }
    }
    
    public void deserializeSelf(Class<?> stopBefore, IObjectDeserializer deserializer) {
        super.deserializeSelf(TargetIntArray.class, deserializer);
        int size = deserializer.readInt();
        this.array = new int[size];
        for (int i = 0; i < size; ++i) {
            this.array[i] = deserializer.readInt();
        }
    }
}
