package Encoding.Binary.Test;

import Encoding.Binary.AutoEncoder;
import Encoding.Binary.BinaryEncodable;
import Encoding.Binary.Serialize;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.nio.ByteBuffer;

@ToString
@AllArgsConstructor()
public class RPCID implements BinaryEncodable<RPCID> {
    @Serialize
    public long id;
    @Serialize
    public String string ;



    private static final AutoEncoder<RPCID> encoder = new AutoEncoder<>(RPCID.class);
    public RPCID(ByteBuffer buffer) {
        encoder.decode(this, buffer);
    }

    @Override
    public AutoEncoder<RPCID> getEncoder() {
        return encoder;
    }
}
