package RAFT.RPC.TCPSocket;

import RAFT.RAFT.Raft;
import RAFT.RPC.Type.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

@Slf4j
public class RPCManagerTCP implements Runnable {

    Raft r;
    ServerSocketChannel serverSocketChannel;
    SocketChannel socketChannel;

    public RPCManagerTCP(Raft r) throws IOException {
        this.r = r;

    }

    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(r.getConfig().getId().getPort()));
            while (true) {
                socketChannel = serverSocketChannel.accept();
                ByteBuffer buffer= ByteBuffer.allocate(4);
                buffer.clear();
                socketChannel.read(buffer);
                buffer.flip();
                int function=buffer.getInt();
                handleRPC(function, socketChannel);
                socketChannel.close();
            }
        } catch (IOException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    void handleRPC(@NonNull int type, SocketChannel chan) throws IOException {

        switch (type) {
            case RPC.HEARTBEAT -> {
                HeartBeatRequest req = new HeartBeatRequest(chan);
                HeartBeatResponse resp = r.sendHeartBeat(req);
                resp.put(chan);
            }
            case RPC.REQUEST_VOTE -> {
                RequestVoteRequest req = new RequestVoteRequest(chan);
                RequestVoteResponse resp = r.requestVote(req);
                resp.put(chan);
            }
            default -> {
                r.getLogger().logf("INVALID RPC FUNCTION[%d]\n", type);

            }
        }
    }
}
