package RAFT;

import RAFT.RAFT.Logs.Log;

@FunctionalInterface
public interface ClientUpdate {
    void update(Log l);
}
