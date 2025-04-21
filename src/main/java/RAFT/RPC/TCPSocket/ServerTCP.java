package RAFT.RPC.TCPSocket;

import RAFT.RAFT.ID;
import RAFT.RPC.*;
import RAFT.RPC.Type.*;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
        TcpImpl.RPC(RPC_TYPE.HEARTBEAT,id, req, res);
        return res;
    }

    @Override
    public RequestVoteResponse requestVote(RequestVoteRequest req) {
        RequestVoteResponse res = new RequestVoteResponse();
        TcpImpl.RPC(RPC_TYPE.REQUEST_VOTE,id, req, res);
        return res;
    }
}
