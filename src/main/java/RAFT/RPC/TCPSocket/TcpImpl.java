package RAFT.RPC.TCPSocket;

import Encoding.Binary.AutoEncoder;
import Encoding.Binary.BinaryEncodable;
import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.RPCMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;

public class TcpImpl {
    public static <REQ extends RPCMessage, RES extends RPCMessage> boolean RPC(int function, ID id, REQ req, RES res) throws IOException {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(id.getHost(), id.getPort()));
        } catch (ClosedByInterruptException e) {
            System.out.println("CONNECTTION INTERRUPTED");
            return false;
        } catch (SocketException e) {
            //COULD NOT CONNECT
//            System.out.println("SOCKET CLOSED UNEXPECTEDLY:");
            return false;
        }
        try {
            //PUT FUNCTION NUMBER
            ByteBuffer out = ByteBuffer.allocate(4);
            out.clear();
            out.putInt(function);
            out.flip();
            socketChannel.write(out);
            req.put(socketChannel);
            res.get(socketChannel);
        } catch (SocketException e) {
            System.out.println("SOCKET CLOSED UNEXPECTEDLY:\t");
            return false;
        } finally {
            socketChannel.close();
        }
        return true;
    }

    @Deprecated
    public static <REQ extends BinaryEncodable<REQ>, RES extends BinaryEncodable<RES>> boolean RPC(int function, ID id, REQ req, RES res) throws IOException {
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(id.getHost(), id.getPort()));
        } catch (ClosedByInterruptException e) {
            System.out.println("CONNECTTION INTERRUPTED");
            return false;
        } catch (SocketException e) {
            //COULD NOT CONNECT
//            System.out.println("SOCKET CLOSED UNEXPECTEDLY:");
            return false;
        }
        try {
            //PUT FUNCTION NUMBER
            ByteBuffer out = ByteBuffer.allocate(4);
            out.clear();
            out.putInt(function);
            out.flip();
            socketChannel.write(out);
            ByteBuffer buffer = req.getEncoder().allocate(req);
            req.getEncoder().encode(req, buffer);
//            new AutoEncoder<>(res.getClass()).decode(buffer);
//            res.get(socketChannel);
        } catch (SocketException e) {
            System.out.println("SOCKET CLOSED UNEXPECTEDLY:\t");
            return false;
        } finally {
            socketChannel.close();
        }
        return true;
    }

}
