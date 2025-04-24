package RAFT.RPC.TCPSocket;

import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.RPCMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
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


        } catch (IOException e) {
            // COULD NOT CONNECT
//            System.out.println();
            return false;
        }
        try {
            req.put(socketChannel);
            res.get(socketChannel);
        } catch (SocketException e) {
//            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

}
