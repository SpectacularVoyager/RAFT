package RAFT.RAFT.RPCType;

import RAFT.RPC.Type.RPCMessage;
import RAFT.RPC.Type.RPCString;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


public class ID implements RPCMessage {
    @Getter
    private long id;
    @Getter
    private int port;
    private RPCString host;

    public String getHost() {
        return host.getS();
    }
    public ID(long id,int port,String host){
        this.id=id;
        this.port=port;
        this.host=new RPCString(host);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ID) {
            return ((ID) obj).id == this.id;
        }
        return false;
    }

    @Override
    public void put(ByteChannel channel) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(12);
        out.clear();
        out.putLong(id);
        out.putInt(port);
        //ENTRIES
        out.flip();
        channel.write(out);
        host.put(channel);

    }
    public ID(ByteChannel c) throws IOException {
        get(c);
    }
    @Override
    public void get(ByteChannel channel) throws IOException {
        ByteBuffer in = ByteBuffer.allocate(12);

        in.clear();
        channel.read(in);
        in.flip();
        id = in.getLong();
        port = in.getInt();
        host=new RPCString(channel);
    }


    @Override
    public String toString() {
//        return String.format("[%d  ->  %s:%d]",id,host,port);
        return String.format("[%d][%s:%d]",id,host,port);
    }
}
