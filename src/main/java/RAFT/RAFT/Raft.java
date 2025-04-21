package RAFT.RAFT;

import Logging.AnsiColor;
import Logging.RaftLogger;
import RAFT.RPC.*;
import RAFT.RPC.Type.HeartBeatRequest;
import RAFT.RPC.Type.HeartBeatResponse;
import RAFT.RPC.Type.RequestVoteRequest;
import RAFT.RPC.Type.RequestVoteResponse;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Raft implements Server {
    RaftLogger logger = new RaftLogger();

    final Object lock = new Object();

    @Getter
    private volatile ID id;
    private volatile RaftState state = RaftState.FOLLOWER;
    private volatile long term = 0;
    private volatile ID votedFor = null;
    ScheduledThreadPoolExecutor executor;
    List<Server> servers;

    public void startElection() {
        for (Server x : servers) {
            //SEND REQUEST VOTE
        }
    }

    public void sendHeartBeats() {
        for (Server x : servers) {
            //SEND HEARTBEAT
        }
    }

    @Override
    public @NonNull HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        return new HeartBeatResponse(this.term,true);
    }

    @Override
    public @NonNull RequestVoteResponse requestVote(RequestVoteRequest req) {
        return new RequestVoteResponse(this.term,true);
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
        executor.schedule(this::mainLoop, getDelay(), TimeUnit.MILLISECONDS);

    }

    public Raft() {
        changeState(RaftState.FOLLOWER);
        logger.log("STARTING RAFT");
        try {
            executor = new ScheduledThreadPoolExecutor(5);
        } catch (IllegalArgumentException e) {
            //LOG FATAL N CANNOT BE LESSER THAN 0
            throw new RuntimeException("THREAD SIZE < 0");
        }
        executor.schedule(this::mainLoop, 150, TimeUnit.MILLISECONDS);
    }

    public synchronized long getDelay() {
        switch (state) {
            case CANDIDATE, RaftState.FOLLOWER -> {
                return 150 + (int) (Math.random() * 150);
            }
            case LEADER -> {
                return 100;
            }
            default -> {
                throw new RuntimeException("UNEXPECTED STATE:" + state);
            }
        }
    }

    public synchronized void voteFor(ID id) {
        this.votedFor = id;
    }

    public synchronized void changeState(RaftState state) {
        switch (state) {
            case FOLLOWER -> {
                logger.setFormat(AnsiColor.WHITE, 0, 1);
            }
            case CANDIDATE -> {
                logger.setFormat(AnsiColor.GREEN, 0, 1);
            }
            case LEADER -> {
                logger.setFormat(AnsiColor.BLUE, 0, 1);
            }
        }
        logger.log("CHANGED STATE TO:" + state);
        this.state = state;
    }

}
