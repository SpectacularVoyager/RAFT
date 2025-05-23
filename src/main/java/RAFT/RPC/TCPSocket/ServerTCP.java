package RAFT.RPC.TCPSocket;

import RAFT.RAFT.RPCType.*;
import RAFT.RPC.*;
import RAFT.RPC.Type.*;
import lombok.Getter;

import java.io.IOException;
import java.util.Optional;

@Getter
public class ServerTCP implements RPCServer {
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
    public Optional<HeartBeatResponse> sendHeartBeat(HeartBeatRequest req) throws IOException {
        HeartBeatResponse res = new HeartBeatResponse();
        if (TcpImpl.RPC(RPC.HEARTBEAT, id, req, res)) {
            return Optional.of(res);
        }
        return Optional.empty();
    }

    @Override
    public Optional<RequestVoteResponse> requestVote(RequestVoteRequest req) throws IOException {
        RequestVoteResponse res = new RequestVoteResponse();
        if (TcpImpl.RPC(RPC.REQUEST_VOTE, id, req, res)) {
            return Optional.of(res);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UpdateResponse> update(RPCString req) throws IOException {
        UpdateResponse res = new UpdateResponse();
        if (TcpImpl.RPC(RPC.UPDATE, id, req, res)) {
            return Optional.of(res);
        }
        return Optional.empty();
    }

    @Override
    public Optional<RaftStatus> status() throws IOException {
        RaftStatus res = new RaftStatus();
        if (TcpImpl.RPC(RPC.STATUS, id, new RPCEmpty(), res)) {
            return Optional.of(res);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
