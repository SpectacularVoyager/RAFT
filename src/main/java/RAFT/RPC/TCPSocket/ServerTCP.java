package RAFT.RPC.TCPSocket;

import RAFT.RAFT.ID;
import RAFT.RPC.*;
import RAFT.RPC.Type.*;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ServerTCP implements Server {
    public ServerTCP(ID id) {
        this.id = id;
    }

    @Getter
    private final ID id;

    private <REQ extends RPCMessage, RES extends RPCMessage> boolean RPC(REQ req, RES res) {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(id.getHost(), id.getPort()));

        } catch (IOException _) {
            // COULD NOT CONNECT
            return false;
        }
        try {
            req.put(socketChannel);
            res.get(socketChannel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        HeartBeatResponse res = new HeartBeatResponse();
        RPC(req, res);
        return res;
    }

    @Override
    public RequestVoteResponse requestVote(RequestVoteRequest req) {
        RequestVoteResponse res = new RequestVoteResponse();
        RPC(req, res);
        return res;
    }
}
