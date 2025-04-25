package Logging;

import RAFT.RAFT.Raft;
import lombok.AllArgsConstructor;

public class RaftLogger {

    AnsiColor color = AnsiColor.DEFAULT;

    public void setFormat(AnsiColor color, int bold, int high) {
        this.color = color;
        if (bold != 0 && bold != 1) {
            bold = 0;
        }
        System.out.printf("\u001b[%d;%dm", bold, color.getCode() + 60 * high);
    }

    public void log(String s) {
        System.out.println(s);
    }

    public void log(AnsiColor col, String s) {
        setFormat(col,0,0);
        System.out.println(s);
        setFormat(color,0,0);
    }

    public void logf(String s, Object... args) {
        System.out.printf(s, args);
    }
}
