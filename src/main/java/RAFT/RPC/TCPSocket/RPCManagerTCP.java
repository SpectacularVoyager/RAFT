package RAFT.RPC.TCPSocket;

import RAFT.RAFT.Raft;
import RAFT.RPC.Type.HeartBeatRequest;
import RAFT.RPC.Type.HeartBeatResponse;
import RAFT.RPC.Type.RPC_TYPE;
import lombok.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
            serverSocketChannel.socket().bind(new InetSocketAddress(r.getConfig().getPort()));
            while (true) {
                socketChannel = serverSocketChannel.accept();
                System.out.println("BRRUG");
                RPC_TYPE type = RPC_TYPE.NONE;
                type.get(socketChannel);
                handleRPC(type, socketChannel);
                socketChannel.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void handleRPC(@NonNull RPC_TYPE type, SocketChannel chan) {
        switch (type) {
            case HEARTBEAT -> {

                HeartBeatRequest req = new HeartBeatRequest(chan);
                HeartBeatResponse resp = r.sendHeartBeat(req);
            }
            case REQUEST_VOTE -> {

            }
            default -> {
                r.getLogger().logf("INVALID RPC FUNCTION[%d]", type.get());
            }
        }
    }
}
