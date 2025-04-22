package Logging;

import RAFT.RAFT.Logs.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppendOnlyLog {
    RandomAccessFile file;
    FileChannel channel;

    public AppendOnlyLog(String p) throws IOException {
        Files.createDirectories(Path.of(p));
        this.file = new RandomAccessFile(Path.of(p, "log.log").toString(), "rw");
//        file.seek(file.length());
        channel = file.getChannel();
    }

    public void writeLog(Log l) throws IOException {
        l.put(channel);
    }

    public List<Log> loadInitial() throws IOException {
        List<Log> l = new ArrayList<>();
        while (file.getFilePointer()<file.length()) {
            l.add(new Log(channel));
        }
        return l;
    }
}
