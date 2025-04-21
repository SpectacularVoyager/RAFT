package RAFT.RPC.TCPSocket;

import RAFT.RPC.Type.ID;
import RAFT.RPC.*;
import RAFT.RPC.Type.*;
import lombok.Getter;

public class ServerTCP implements Server {
    public ServerTCP(ID id) {
        this.id = id;
    }

    @Getter
    private final ID id;

    private static int RPC_ID = 1;



    @Override
    public HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        HeartBeatResponse res = new HeartBeatResponse();
        TcpImpl.RPC(RPC.HEARTBEAT,id, req, res);
        return res;
    }

    @Override
    public RequestVoteResponse requestVote(RequestVoteRequest req) {
        RequestVoteResponse res = new RequestVoteResponse();
        TcpImpl.RPC(RPC.REQUEST_VOTE,id, req, res);
        return res;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
