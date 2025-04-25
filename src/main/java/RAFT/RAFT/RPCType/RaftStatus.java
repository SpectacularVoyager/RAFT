package RAFT.RAFT.RPCType;

import RAFT.RAFT.Logs.Log;
import RAFT.RAFT.RaftState;
import RAFT.RPC.Type.RPCMessage;
import RAFT.RPC.Type.RPCString;
import lombok.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RaftStatus implements RPCMessage {
    ID id;
    RPCString state;
    long term;
    long commited;
    List<Log> logs;

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(24);
        id.put(channel);
        state.put(channel);

        out.clear();
        out.putLong(term);
        out.putLong(commited);
        out.putLong(logs.size());
        out.flip();
        for (var l : logs) l.put(channel);
        channel.write(out);
    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(24);
        id = new ID(channel);
        state = new RPCString(channel);
        in.clear();
        channel.read(in);
        in.flip();
        term = in.getLong();
        commited = in.getLong();
        long len = in.getLong();
        logs = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            logs.add(new Log(channel));
        }
    }
}
