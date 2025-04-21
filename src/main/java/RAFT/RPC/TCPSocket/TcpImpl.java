package RAFT.RPC.TCPSocket;

import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.RPCMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TcpImpl {
    public static <REQ extends RPCMessage, RES extends RPCMessage> boolean RPC(int function, ID id, REQ req, RES res) {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(id.getHost(), id.getPort()));

            //PUT FUNCTION NUMBER
            ByteBuffer out = ByteBuffer.allocate(4);
            out.clear();
            out.putInt(function);
            out.flip();
            socketChannel.write(out);


        } catch (IOException _) {
            // COULD NOT CONNECT
//            System.out.println();
            return false;
        }
        try {
            req.put(socketChannel);
            res.get(socketChannel);
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return true;
    }

}
