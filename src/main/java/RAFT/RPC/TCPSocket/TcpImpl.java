package RAFT.RPC.TCPSocket;

import RAFT.RAFT.ID;
import RAFT.RPC.Type.RPCMessage;
import RAFT.RPC.Type.RPC_TYPE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TcpImpl {
    public static <REQ extends RPCMessage, RES extends RPCMessage> boolean RPC(RPC_TYPE function, ID id, REQ req, RES res) {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();

            //PUT FUNCTION NUMBER
            ByteBuffer out = ByteBuffer.allocate(8);
            out.clear();
            out.putLong(function.get());
            out.flip();
            socketChannel.write(out);

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
    public static <REQ extends RPCMessage, RES extends RPCMessage> boolean RECIEVE(RPC_TYPE function, ID id, REQ req, RES res) {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();

            //PUT FUNCTION NUMBER
            ByteBuffer out = ByteBuffer.allocate(8);
            out.clear();
            out.putLong(function.get());
            out.flip();
            socketChannel.write(out);

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

}
