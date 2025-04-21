package RAFT.RAFT;

import RAFT.RPC.Server;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Raft {

    final Object lock = new Object();

    @Getter
    private volatile ID id;
    @Getter
    private volatile RaftState state = RaftState.FOLLOWER;
    @Getter
    private volatile int term = 0;
    @Getter
    private volatile ID votedFor = null;
    ScheduledThreadPoolExecutor executor;
    List<Server> servers;

    public synchronized void voteFor(ID id) {
        this.votedFor = id;
    }

    public synchronized void changeState(RaftState state) {
        this.state = state;
    }

    public void startElection() {
        for(Server x:servers){
            //SEND REQUEST VOTE
        }

    }

    public void sendHeartBeats() {
        for(Server x:servers){
            //SEND HEARTBEAT
        }
    }

    private void mainLoop() {
        switch (state) {
            case LEADER -> {

            }
            case CANDIDATE -> {
                synchronized (lock) {
                    term++;
                }
            }
            case FOLLOWER -> {
                synchronized (lock) {
                    term++;
                    voteFor(this.id);
                    changeState(RaftState.CANDIDATE);
                }
            }
        }
    }

    public Raft() {
        System.out.println("STARTING RAFT");
        try {
            executor = new ScheduledThreadPoolExecutor(5);
        } catch (IllegalArgumentException e) {
            //LOG FATAL N CANNOT BE LESSER THAN 0
            return;
        }
        executor.schedule(() -> {
            System.out.println("LEADER NOT FOUND");
        }, 150, TimeUnit.MILLISECONDS);
    }
}
