package RAFT.RPC;

import RAFT.RPC.Type.*;

import java.io.IOException;
import java.util.Optional;

public interface RPCServer {
    ID getId();
    long getLogIndex();
    void setLogIndex(long c);
    Optional<HeartBeatResponse> sendHeartBeat(HeartBeatRequest req) throws IOException;
    Optional<RequestVoteResponse> requestVote(RequestVoteRequest req) throws IOException;
    Optional<UpdateResponse> update(RPCString string) throws IOException;

}
