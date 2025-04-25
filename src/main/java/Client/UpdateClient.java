package Client;

import Logging.AnsiColor;
import Logging.RaftLogger;
import RAFT.RPC.RPCServer;
import RAFT.RPC.Server;
import RAFT.RPC.ServerFactory;
import RAFT.RPC.TCPSocket.ServerTCP;
import RAFT.RPC.Type.RPCString;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;

public class UpdateClient {
    RaftLogger log = new RaftLogger();

    public static void main(String[] args) throws IOException {
        new UpdateClient();
    }

    UpdateClient() throws IOException {
        RPCServer server = ServerFactory.getServer("tcp/localhost:8000", 1);
        Scanner in = new Scanner(System.in);
        log.log("TYPE MESSAGE TO SEND TO:\t" + server);
        log.setFormat(AnsiColor.GREEN, 0, 0);
        log.setFormat(AnsiColor.WHITE, 0, 0);
        while (true) {
            log.setFormat(AnsiColor.GREEN, 0, 0);
            System.out.print("raft> ");
            log.setFormat(AnsiColor.WHITE, 0, 0);

            String line = in.nextLine();
            if (line.startsWith("changeaddr")) {
                System.out.println(line);
                server= ServerFactory.getServer(line.split(" ")[1], 1);
                log.setFormat(AnsiColor.GREEN, 0, 0);
                System.out.println("CHANGING SERVER ADDRESS TO:\t"+server);
                continue;
            }
            var v = server.update(new RPCString(line));
            if (v.isEmpty()) {
                log.setFormat(AnsiColor.RED, 0, 0);
                System.out.println("COULD NOT REACH SERVER");
            } else if (v.get().isSuccess()) {
                log.setFormat(AnsiColor.GREEN, 0, 0);
                System.out.println(v);
            } else {
                log.setFormat(AnsiColor.YELLOW, 0, 0);
                System.out.println(v);
            }
        }
    }


    void setColor() {

    }
}
