package RAFT.RPC.Type;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public enum RPC_TYPE implements RPCMessage {
    NONE(0),
    HEARTBEAT(1),
    REQUEST_VOTE(2);
    long x;

    RPC_TYPE(long x) {
        this.x = x;
    }

    public long get() {
        return x;
    }

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(8);
        out.clear();
        out.putLong(x);
        out.flip();
        channel.write(out);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(8);
        in.clear();
        channel.read(in);
        in.flip();
        x = in.getLong();
    }
}
