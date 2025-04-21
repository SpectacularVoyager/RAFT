package Logging;

import RAFT.RAFT.Raft;
import lombok.AllArgsConstructor;

public class RaftLogger {

    public void setFormat(AnsiColor color,int bold,int high){
        if(bold!=0&&bold!=1){
            bold=0;
        }
        System.out.printf("\u001b[%d;%dm",bold,color.getCode()+60*high);
    }
    public void log(String s){
        System.out.println(s);
    }
    public void logf(String s,Object ... args){
        System.out.printf(s,args);
    }
}
