package RAFT.RPC.TCPSocket;

import RAFT.RAFT.RPCType.*;
import RAFT.RAFT.Raft;
import RAFT.RPC.Type.*;
import lombok.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;


public class RPCManagerTCP implements Runnable {

    Raft r;
    ServerSocketChannel serverSocketChannel;
    SocketChannel socketChannel;
    ExecutorService executor;

    public RPCManagerTCP(Raft r, ExecutorService service) throws IOException {
        this.r = r;
        this.executor = service;

    }

    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(r.getConfig().getId().getPort()));
            while (true) {
                socketChannel = serverSocketChannel.accept();
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.clear();
                socketChannel.read(buffer);
                buffer.flip();
                int function = buffer.getInt();
//                executor.execute();
                handleRPC(function, socketChannel);
                socketChannel.close();
            }
        } catch (BufferUnderflowException e) {
            System.out.println(e);
            System.out.println("EXPECTED BUFFER IN CHANNEL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handleRPC(@NonNull int type, SocketChannel chan) throws IOException {
        try {
            switch (type) {
                case RPC.HEARTBEAT -> {
                    HeartBeatRequest req = new HeartBeatRequest(chan);
                    HeartBeatResponse resp = r.receiveHeartBeat(req);
                    resp.put(chan);
                }
                case RPC.REQUEST_VOTE -> {
                    RequestVoteRequest req = new RequestVoteRequest(chan);
                    RequestVoteResponse resp = r.receiveRequestVote(req);
                    resp.put(chan);
                }
                case RPC.UPDATE -> {
                    RPCString req = new RPCString(chan);
                    UpdateResponse resp = r.update(req);
                    resp.put(chan);
                }
                default -> {
                    r.getLogger().logf("INVALID RPC FUNCTION[%d]\n", type);

                }
            }
        } finally {
            chan.close();
        }
    }
}
