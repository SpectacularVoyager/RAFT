import RAFT.RAFT.Config;
import RAFT.RAFT.Raft;
import RAFT.RPC.Server;
import RAFT.RPC.TCPSocket.ServerTCP;
import RAFT.RPC.Type.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
//        Logger logger = LoggerFactory.getLogger(Main.class);
//        logger.info("Example log from {}", Main.class.getSimpleName());//

        List<ID> l = List.of(new ID(123, 8001, "localhost"));
        Raft r1 = new Raft(new Config(new ID(1000,8000,"localhost"), List.of()));
        Raft r2 = new Raft(new Config(new ID(1000,8001,"localhost"), l.stream().map(ServerTCP::new).map(x->(Server)x).toList()));
        System.out.println("EY");
    }
}
