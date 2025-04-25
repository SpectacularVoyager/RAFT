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
public class RequestVoteRequest implements RPCMessage {
    ID id;
    long term;
    long lastLogIndex;
    long lastLogTerm;

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(24);
        id.put(channel);
        out.clear();
        out.putLong(term);
        out.putLong(lastLogIndex);
        out.putLong(lastLogTerm);
        out.flip();
        channel.write(out);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(24);
        id = new ID(channel);
        in.clear();
        channel.read(in);
        in.flip();
        term = in.getLong();
        lastLogIndex = in.getLong();
        lastLogTerm = in.getLong();
    }

    public RequestVoteRequest(ByteChannel channel) throws IOException {
        get(channel);
    }
}
