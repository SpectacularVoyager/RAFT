package Encoding.Binary.Test;

import Encoding.Binary.AutoEncoder;
import Encoding.Binary.BinaryEncodable;
import Encoding.Binary.Serialize;

public abstract class BasicEncodable<T> implements BinaryEncodable<T> {
    AutoEncoder<T> encoder;

    public BasicEncodable(AutoEncoder<T> encoder) {
        this.encoder = encoder;
    }

    @Override
    public AutoEncoder<T> getEncoder() {
        return encoder;
    }
}
