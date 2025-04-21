package RAFT.RAFT.Logs;

import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.RPCMessage;
import RAFT.RPC.Type.RPCString;
import lombok.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
@Getter@Setter@ToString@AllArgsConstructor@NoArgsConstructor
public class Log implements RPCMessage {
    long index;
    long term;
    RPCMessage message;

    @Override
    public void put(ByteChannel channel) throws IOException {

        ByteBuffer out = ByteBuffer.allocate(16);
        out.clear();
        out.putLong(index);
        out.putLong(term);
        //ENTRIES
        out.flip();
        channel.write(out);
        message.put(channel);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {

        ByteBuffer in = ByteBuffer.allocate(16);
        in.clear();
        channel.read(in);
        in.flip();
        index = in.getLong();
        term = in.getLong();

        message=new RPCString(channel);
        //ENTRIES
    }

    public Log(ByteChannel channel) throws IOException {
        get(channel);
    }

}
