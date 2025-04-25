package Client;

import RAFT.RPC.RPCServer;
import RAFT.RPC.Server;
import RAFT.RPC.ServerFactory;
import RAFT.RPC.Type.RPCString;

import java.io.IOException;
import java.util.Scanner;

public class UpdateClient {

    public static void main(String[] args) throws IOException {
        new UpdateClient();
    }

    UpdateClient() throws IOException {
        RPCServer server = ServerFactory.getServer("tcp/localhost:8000", 1);
        Scanner in = new Scanner(System.in);
        System.out.println("TYPE MESSAGE TO SEND TO:\t"+server);
        while (in.hasNext()) {
            System.out.println("raft> ");
            String line = in.nextLine();
            System.out.println(server.update(new RPCString(line)));
        }
    }
}
