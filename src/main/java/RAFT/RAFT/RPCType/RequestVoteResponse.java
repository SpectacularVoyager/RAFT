package RAFT.RAFT.RPCType;

import RAFT.RPC.Type.RPCMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteResponse implements RPCMessage {
    long term;
    boolean voteGranted;

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(9);
        out.clear();
        out.putLong(term);
        out.put(voteGranted ? (byte) 1 : 0);
        out.flip();
        channel.write(out);
    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(9);
        in.clear();
        channel.read(in);
        in.flip();
        term = in.getLong();
        voteGranted = in.get() != 0;
    }
}
