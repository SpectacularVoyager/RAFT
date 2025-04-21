package RAFT.RPC;

import RAFT.RPC.Type.*;

public interface Server {
    ID getId();
    long getLogIndex();
    void setLogIndex(long c);
    HeartBeatResponse sendHeartBeat(HeartBeatRequest req);
    RequestVoteResponse requestVote(RequestVoteRequest req);
    UpdateResponse update(RPCString string);
}
