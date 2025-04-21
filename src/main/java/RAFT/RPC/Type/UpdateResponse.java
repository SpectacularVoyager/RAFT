package RAFT.RPC.Type;

import RAFT.RAFT.Logs.Log;
import lombok.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;

import RAFT.RPC.Type.RPCMessage;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UpdateResponse implements RPCMessage {
    long index;
    long term;
    boolean success;
    ID res;

    UpdateResponse(ByteChannel channel) throws IOException {
        get(channel);
    }

    public UpdateResponse(Log l) {
        this.index = l.getIndex();
        this.term = l.getTerm();
        res = new ID(0, 0, "");
        this.success = true;
    }

    public UpdateResponse(ID res) {
        this.index = -1;
        this.term = -1;
        this.success = false;
        this.res = res;
    }

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.clear();
        buffer.putLong(index);
        buffer.putLong(term);
        buffer.put(success ? (byte) 1 : 0);
        buffer.flip();
        channel.write(buffer);
        if (res == null) res = new ID(0, 0, "");
        res.get(channel);


    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.clear();
        index = buffer.getLong();
        term = buffer.getLong();
        success = buffer.get() != 0;
        buffer.flip();
        channel.write(buffer);
        res.get(channel);
    }
}

