package RAFT.RPC;

import RAFT.RAFT.RPCType.*;
import RAFT.RPC.Type.*;

public interface Server {
    ID getId();
    HeartBeatResponse receiveHeartBeat(HeartBeatRequest req);
    RequestVoteResponse receiveRequestVote(RequestVoteRequest req);
    UpdateResponse update(RPCString string);
}
