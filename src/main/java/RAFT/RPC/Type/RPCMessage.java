package RAFT.RPC.Type;

import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;

public interface RPCMessage {
    void put(ByteChannel channel) throws IOException;
    void get(ByteChannel channel) throws IOException;
}
