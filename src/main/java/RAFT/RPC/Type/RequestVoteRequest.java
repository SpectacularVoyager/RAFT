package RAFT.RPC.Type;

import java.io.IOException;
import java.nio.channels.ByteChannel;

public class RequestVoteRequest implements RPCMessage{
    @Override
    public void put(ByteChannel channel) throws IOException {

    }

    @Override
    public void get(ByteChannel channel) throws IOException {

    }
}
