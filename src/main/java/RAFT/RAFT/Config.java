package RAFT.RAFT;

import RAFT.RPC.RPCServer;
import RAFT.RPC.Server;
import RAFT.RPC.ServerFactory;
import RAFT.RPC.Type.ID;
import RAFT.RPC.Type.RPC;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Config {
    ID id;
    List<RPCServer> servers = new ArrayList<>();
    JSONParser parser = new JSONParser();

    public void addServer(JSONObject s) throws ParseException {

        servers.add(ServerFactory.getServer((String) s.get("address"), (Long) s.get("id")));
    }

    public Config(String s, long _id) throws ParseException {


        JSONObject c = (JSONObject) parser.parse(s);
        for (Object x : (JSONArray) c.get("servers")) {
            addServer((JSONObject) x);
        }

        var t = servers.stream().filter(x -> x.getId().getId() == _id).findAny();
        servers = servers.stream().filter(x -> x.getId().getId() != _id).toList();
        if (t.isEmpty()) {
            throw new RuntimeException("INVALID ID:" + _id);
        }
        id = t.get().getId();
    }

    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", servers=" + servers +
                '}';
    }
}
