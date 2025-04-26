package RAFT.RPC;

import RAFT.RPC.TCPSocket.ServerTCP;
import RAFT.RAFT.RPCType.ID;

import java.util.Arrays;

public class ServerFactory {
    public static RPCServer getServer(String name, long id) {
        name = name.replace(" ", "");
        if (!name.contains("/")) {
            return new ServerTCP(fromString(name, id));
        } else {
            String[] split = name.split("/");
            if (split[0].equals("tcp")) {
                return new ServerTCP(fromString(split[1], id));
            } else {
                throw new UnsupportedOperationException("ONLY SUPPORTS TCP");
            }
        }
    }

    private static ID fromString(String name, long id) {
        String[] s = name.split(":");
        if (Arrays.stream(s).count() != 2) {
            System.out.println("INVALID FORMAT:EXPECTED [HOST]:[PORT]");
        }
        int x;
        try {
            x = Integer.parseInt(s[1]);
        } catch (Exception e) {
            System.out.println("INVALID PORT NUMBER:\t" + s[1]);
            return null;
        }
        return new ID(id, x, s[0]);

    }
}
