package RAFT.RPC;

import RAFT.RAFT.ID;

public interface Server {
    ID getID();
    void ping();
    HeartBeatResponse sendHeartBeat(HeartBeatRequest req);
    RequestVoteResponse requestVote(RequestVoteRequest req);
}
