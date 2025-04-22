import RAFT.RAFT.Config;
import RAFT.RAFT.Raft;
import RAFT.RPC.Server;
import RAFT.RPC.TCPSocket.ServerTCP;
import RAFT.RPC.Type.ID;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, ParseException {
        System.out.println(List.of(args));
        String data;
        long id;
        if (args.length < 2) {
            System.out.println("USAGE: RAFT [CONFIG] SERVER_ID");
//            return;

            data = """
                    {
                       "logfile": "log",
                       "servers": [
                         {"address": "tcp/localhost:8000","id":1},
                         {"address": "tcp/localhost:8001","id":2},
                         {"address": "tcp/localhost:8002","id":3}
                       ]
                     }
                    """;
            id = 1;

        } else {
            System.out.println(args[0]);
            data = Files.readString(Path.of(args[0]));
            try {
                id = Long.parseLong(args[1]);
            } catch (Exception e) {
                System.out.println("CANNOT PARSE NON INTEGER ID:\t" + args[1]);
                return;
            }
        }


        Config conf = new Config(data, id);
        System.out.println(conf);
        Raft r = new Raft(conf);
    }
}
