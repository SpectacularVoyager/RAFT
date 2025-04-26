package client.java;

import RAFT.ClientUpdate;
import RAFT.RAFT.Config;
import RAFT.RAFT.Logs.Log;
import RAFT.RAFT.Raft;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientMain {

    public static void main(String[] args) throws IOException, ParseException {
        new ClientMain("res/config.json", 1);
    }

    public ClientMain(String file, long id) throws IOException, ParseException {
        Config conf = new Config(Files.readString(Path.of(file)), id);
        Raft r = new Raft(conf);
        System.out.println("HEY");
        r.setClientUpdate(this::update);
    }

    void update(Log l) {
        System.out.println("UPDATE:\t"+l);
    }
}
