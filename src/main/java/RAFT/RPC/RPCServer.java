package RAFT.RPC;

import RAFT.RPC.Type.*;

import java.util.Optional;

public interface RPCServer {
    ID getId();
    long getLogIndex();
    void setLogIndex(long c);
    Optional<HeartBeatResponse> sendHeartBeat(HeartBeatRequest req);
    Optional<RequestVoteResponse> requestVote(RequestVoteRequest req);
    Optional<UpdateResponse> update(RPCString string);

}
