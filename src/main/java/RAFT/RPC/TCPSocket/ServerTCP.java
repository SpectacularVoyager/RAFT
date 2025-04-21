package RAFT.RPC.TCPSocket;

import RAFT.RPC.Type.ID;
import RAFT.RPC.*;
import RAFT.RPC.Type.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ServerTCP implements Server {
    long logIndex;

    public ServerTCP(ID id) {
        this.id = id;
    }

    private final ID id;

    @Override
    public void setLogIndex(long c) {
        this.logIndex = c;
    }

    @Override
    public HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        HeartBeatResponse res = new HeartBeatResponse();
        TcpImpl.RPC(RPC.HEARTBEAT, id, req, res);
        return res;
    }

    @Override
    public RequestVoteResponse requestVote(RequestVoteRequest req) {
        RequestVoteResponse res = new RequestVoteResponse();
        TcpImpl.RPC(RPC.REQUEST_VOTE, id, req, res);
        return res;
    }

    @Override
    public UpdateResponse update(RPCString req) {
        UpdateResponse res = new UpdateResponse();
        TcpImpl.RPC(RPC.UPDATE, id, req, res);
        return res;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
