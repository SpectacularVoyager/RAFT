package RAFT.RPC.Type;

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
public class HeartBeatRequest implements RPCMessage{
    ID leaderID;
    long leaderTerm;
    long prevLogIndex;
    long prevLogTerm;
    long leaderCommit;
    //ENTRIES

    @Override
    public void put(ByteChannel channel) throws IOException {
        leaderID.put(channel);
        ByteBuffer out = ByteBuffer.allocate(32);
        out.clear();
        out.putLong(leaderTerm);
        out.putLong(prevLogIndex);
        out.putLong(prevLogTerm);
        out.putLong(leaderCommit);
        //ENTRIES
        out.flip();
        channel.write(out);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        leaderID=new ID(channel);
        ByteBuffer in = ByteBuffer.allocate(32);
        in.clear();
        channel.read(in);
        in.flip();
        leaderTerm=in.getLong();
        prevLogIndex=in.getLong();
        prevLogTerm=in.getLong();
        leaderCommit=in.getLong();
        //ENTRIES
    }
    public HeartBeatRequest(ByteChannel channel) throws IOException {
        get(channel);
    }

    @Override
    public String toString() {
        return "HeartBeatRequest{" +
                "leaderID=" + leaderID +
                ", leaderTerm=" + leaderTerm +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", leaderCommit=" + leaderCommit +
                '}';
    }
}
