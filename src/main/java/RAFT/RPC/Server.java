package RAFT.RPC;

import RAFT.RPC.Type.*;

public interface Server {
    ID getId();
    long getLogIndex();
    void setLogIndex(long c);
    HeartBeatResponse recieveHeartBeat(HeartBeatRequest req);
    RequestVoteResponse recieveRequestVote(RequestVoteRequest req);
    UpdateResponse update(RPCString string);
}
