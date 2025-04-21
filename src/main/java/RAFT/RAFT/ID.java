package RAFT.RAFT;

import RAFT.RPC.Type.RPCMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.ByteChannel;

@Getter
@AllArgsConstructor
public class ID implements RPCMessage {
    private long id;
    private String host;
    private int port;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ID) {
            return ((ID) obj).id == this.id;
        }
        return false;
    }

    @Override
    public void put(ByteChannel channel) throws IOException {

    }

    @Override
    public void get(ByteChannel channel) throws IOException {

    }
}
