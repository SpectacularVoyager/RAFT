package Encoding.Binary;

import Encoding.Binary.Test.RPCID;
import Encoding.Binary.Test.RPCResponse;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class BinaryEncodingTest {
    public static void main(String[] args) {
        AutoEncoder<RPCResponse> encoder = new AutoEncoder<>(RPCResponse.class);
        RPCResponse response = new RPCResponse(1, 2, "Hello World", new RPCID(5, "Hello World"));
        var buffer = encoder.allocate(response);
        encoder.encode(response, buffer);

        RPCResponse r = new RPCResponse();
        buffer.flip();
        encoder.decode(r, buffer);
        System.out.println(r);

    }
}
