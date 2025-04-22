package RAFT.RPC;

import RAFT.RPC.Type.*;

public interface Server {
    ID getId();
    HeartBeatResponse recieveHeartBeat(HeartBeatRequest req);
    RequestVoteResponse recieveRequestVote(RequestVoteRequest req);
    UpdateResponse update(RPCString string);
}
