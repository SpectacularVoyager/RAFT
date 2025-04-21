package RAFT.RAFT;

import RAFT.RPC.Server;
import RAFT.RPC.Type.ID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter@AllArgsConstructor
public class Config {
    ID id;
    List<Server> servers;
}
