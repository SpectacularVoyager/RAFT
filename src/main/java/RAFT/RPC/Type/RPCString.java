package RAFT.RPC.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@AllArgsConstructor
public class RPCString implements RPCMessage {
    String s;

    public RPCString(ByteChannel channel) throws IOException {
        get(channel);
    }

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(s.length() + 4);
        out.clear();
        out.putInt(s.length());
        out.put(s.getBytes(StandardCharsets.UTF_8));
        out.flip();
        channel.write(out);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(4);
        in.clear();
        channel.read(in);
        in.flip();

        int len = in.getInt();

        ByteBuffer sin = ByteBuffer.allocate(len);
        sin.clear();
        channel.read(sin);
        sin.flip();

        s=new String(sin.array());

    }

    @Override
    public String toString() {
        return s;
    }
}
