package RAFT.RPC.Type;

import RAFT.RAFT.ID;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;

@Getter
@AllArgsConstructor
public class HeartBeatRequest implements RPCMessage{
    long leaderTerm;
    ID leaderID;
    long prevLogIndex;
    long prevLogTerm;
    //ENTRIES
    long leaderCommit;

    @Override
    public void put(ByteChannel channel) throws IOException {

    }

    @Override
    public void get(ByteChannel channel) throws IOException {

    }
}
