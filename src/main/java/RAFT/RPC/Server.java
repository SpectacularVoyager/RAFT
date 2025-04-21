package RAFT.RPC;

import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.HeartBeatRequest;
import RAFT.RPC.Type.HeartBeatResponse;
import RAFT.RPC.Type.RequestVoteRequest;
import RAFT.RPC.Type.RequestVoteResponse;

public interface Server {
    ID getId();
    HeartBeatResponse sendHeartBeat(HeartBeatRequest req);
    RequestVoteResponse requestVote(RequestVoteRequest req);
}
