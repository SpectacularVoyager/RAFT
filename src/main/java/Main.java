import RAFT.RAFT.Config;
import RAFT.RAFT.Raft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        Logger logger = LoggerFactory.getLogger(Main.class);
//        logger.info("Example log from {}", Main.class.getSimpleName());//
        Raft r=new Raft(new Config(8000));
    }
}
