package RAFT.RAFT;

import Logging.AnsiColor;
import Logging.RaftLogger;
import RAFT.RPC.*;
import RAFT.RPC.TCPSocket.RPCManagerTCP;
import RAFT.RPC.Type.*;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Raft implements Server {
    @Getter
    RaftLogger logger = new RaftLogger();
    final Object lock = new Object();
    @Getter
    private volatile ID id;
    @Getter
    private volatile Config config;
    private volatile RaftState state = RaftState.FOLLOWER;
    private volatile long term = 0;
    private volatile ID votedFor = null;
    private volatile long commitIndex = 0;

    private volatile long votesRecieved = 0;
    ScheduledThreadPoolExecutor executor;
    List<Server> servers;
    RPCManagerTCP manager;

    private volatile ScheduledFuture<?> timer = null;


    public synchronized boolean isMajority() {
        return (2 * votesRecieved > (this.servers.size() + 1));
    }

    public void sendHeartBeats() {
        for (Server x : servers) {
            //SEND HEARTBEAT
            HeartBeatRequest request = new HeartBeatRequest();
            request.setLeaderCommit(commitIndex);
            request.setLeaderID(id);
            request.setLeaderTerm(term);
            request.setPrevLogIndex(0);
            request.setPrevLogTerm(0);
            var reply = x.sendHeartBeat(request);

            synchronized (lock) {
                if (reply.term > this.term) {
                    changeState(RaftState.FOLLOWER);
                }
            }
        }
    }


    @Override
    public @NonNull HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        synchronized (lock) {
//            logger.log("RECIEVED HEARTBEAT");
//            logger.log(req.toString());

            if (term <= req.getLeaderTerm()) {
                voteFor(null);
                if (state != RaftState.FOLLOWER) {
                    changeState(RaftState.FOLLOWER);
                }
                term = req.getLeaderTerm();
            }
            if (term > req.getLeaderTerm()) {
                return new HeartBeatResponse(this.term, false);
            }
            //ACCEPT LOGS

            //RESET TIMER
            if (timer != null) {
                timer.cancel(false);
            }
            timer = executor.schedule(this::mainLoop, getDelay(), TimeUnit.MILLISECONDS);
            return new HeartBeatResponse(this.term, true);
        }
    }

    public void startElection() {
        for (Server x : servers) {
            //SEND REQUEST VOTE
            RequestVoteRequest request = new RequestVoteRequest();
            request.setId(id);
            request.setTerm(term);
            request.setLastLogIndex(0);
            request.setLastLogTerm(0);
            var reply = x.requestVote(request);
            synchronized (lock) {
                if (reply.isVoteGranted()) {
                    this.votesRecieved++;
                }
            }

        }
        synchronized (lock) {
            if (isMajority()) {
                //CHANGE STATE TO LEADER
                changeState(RaftState.LEADER);
                logger.logf("WON ELECTION WITH (%d/%d) VOTES\n", votesRecieved, servers.size() + 1);
            } else {
                logger.logf("LOST ELECTION WITH (%d/%d) VOTES\n", votesRecieved, servers.size() + 1);

            }
        }
    }

    @Override
    public synchronized @NonNull RequestVoteResponse requestVote(RequestVoteRequest req) {

        synchronized (lock) {
            //LOG STUFF

            if (commitIndex > req.getLastLogIndex()) {
                return rejectVote(req, "OLD LOGS");
            }
            if (term < req.getTerm()) {
                if (state != RaftState.FOLLOWER) {
                    changeState(RaftState.FOLLOWER);
                    voteFor(req.getId());
                }
            } else if (term > req.getTerm()) {
                return rejectVote(req, "OLDER TERM");
            } else {
                if (state != RaftState.FOLLOWER) {
                    return rejectVote(req, "SAME TERM");
                }
            }
            if (votedFor != null && votedFor != req.getId()) {
                return rejectVote(req, "ALREADY VOTING FOR:" + req.getId());
            }
            voteFor(req.getId());
            logger.log("VOTED FOR:" + req.getId());
            return new RequestVoteResponse(this.term, true);
        }
    }

    public RequestVoteResponse rejectVote(RequestVoteRequest x, String s) {
        logger.logf("%s\tREJECTED VOTE [%d] FOR [%s]:\t%s\n", id.toString(), term, x.getId().toString(), s);
        return new RequestVoteResponse(this.term, false);
    }


    private void mainLoop() {
        switch (state) {
            case LEADER -> {
                sendHeartBeats();
            }
            case CANDIDATE -> {
                synchronized (lock) {
                    term++;
                    voteFor(this.id);
                    this.votesRecieved = 1;
                }
                startElection();
            }
            case FOLLOWER -> {
                synchronized (lock) {
                    term++;
                    voteFor(this.id);
                    this.votesRecieved = 1;
                    changeState(RaftState.CANDIDATE);
                }
                startElection();
            }
        }
        synchronized (lock) {
            timer = executor.schedule(this::mainLoop, getDelay(), TimeUnit.MILLISECONDS);
        }

    }

    public Raft(Config config) throws IOException {
        this.id = config.id;
        this.config = config;
        this.servers = this.config.servers;
        changeState(RaftState.FOLLOWER);
        manager = new RPCManagerTCP(this);
        logger.log("STARTING RAFT");
        try {
            executor = new ScheduledThreadPoolExecutor(5);
        } catch (IllegalArgumentException e) {
            //LOG FATAL N CANNOT BE LESSER THAN 0
            throw new RuntimeException("THREAD SIZE < 0");
        }
        executor.schedule(this::mainLoop, 150, TimeUnit.MILLISECONDS);
        executor.execute(manager);
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
        int b=0,h=0;
        switch (state) {
            case FOLLOWER -> {
                logger.setFormat(AnsiColor.WHITE, b, h);
            }
            case CANDIDATE -> {
                logger.setFormat(AnsiColor.GREEN, b,h);
            }
            case LEADER -> {
                logger.setFormat(AnsiColor.BLUE, b,h);
            }
        }
        logger.log(id + "\tCHANGED STATE TO:" + state);
        this.state = state;
    }

}
