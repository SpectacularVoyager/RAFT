package RAFT.RPC.Type;

import RAFT.RAFT.Logs.Log;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HeartBeatRequest implements RPCMessage {
    ID leaderID;
    long leaderTerm;
    long prevLogIndex;
    long prevLogTerm;
    long leaderCommit;
    List<Log> logs=new ArrayList<>();
    //ENTRIES

    @Override
    public void put(ByteChannel channel) throws IOException {
        leaderID.put(channel);
        ByteBuffer out = ByteBuffer.allocate(40);
        out.clear();
        out.putLong(leaderTerm);
        out.putLong(prevLogIndex);
        out.putLong(prevLogTerm);
        out.putLong(leaderCommit);
        out.putLong(logs.size());
        //LOG LENGTH
//        if (logs == null) {
//            out.putLong(0);
//        } else {
//            out.putLong(logs.size());
//        }
        //ENTRIES
        out.flip();
        channel.write(out);
        for (Log x : logs) x.put(channel);

    }

    @Override
    public void get(ByteChannel channel) throws IOException {
        leaderID = new ID(channel);
        ByteBuffer in = ByteBuffer.allocate(40);
        in.clear();
        channel.read(in);
        in.flip();
        leaderTerm = in.getLong();
        prevLogIndex = in.getLong();
        prevLogTerm = in.getLong();
        leaderCommit = in.getLong();
        long len = in.getLong();
        //ENTRIES
        for (long i = 0; i < len; i++) {
            logs.add(new Log(channel));
        }
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
