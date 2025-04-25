package RAFT.RPC.Type;

import RAFT.RAFT.Logs.Log;
import RAFT.RAFT.RPCType.ID;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;

public class RPCEmpty implements RPCMessage {
    @Override
    public void put(ByteChannel channel) throws IOException {

    }

    @Override
    public void get(ByteChannel channel) throws IOException {

    }
}
