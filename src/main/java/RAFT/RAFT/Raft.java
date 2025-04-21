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
    private volatile ScheduledFuture<?> electionBid = null;


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
                    System.out.println(term + "\t" + reply.term);
                    changeState(RaftState.FOLLOWER);
                }
            }
        }
    }


    @Override
    public @NonNull HeartBeatResponse sendHeartBeat(HeartBeatRequest req) {
        synchronized (lock) {
//            logger.log("RECIEVED HEARTBEAT FROM:"+req.getLeaderID());
//            logger.log(req.toString());

            if (term <= req.getLeaderTerm()) {
                voteFor(null);
                if (state != RaftState.FOLLOWER) {
                    changeState(RaftState.FOLLOWER);
                }
                if(term!=req.getLeaderTerm()){
                    logger.logf("UPDATED TERM TO [%d] <- [%d]\n",req.getLeaderTerm(),term);
                }
                term = req.getLeaderTerm();
            }
            if (term > req.getLeaderTerm()) {
                return new HeartBeatResponse(this.term, false);
            }
            //ACCEPT LOGS

            //RESET TIMER
            resetTimers();
            return new HeartBeatResponse(this.term, true);
        }
    }
    synchronized void resetTimers(){
        //RESET TIMER
        if (timer != null) {
            timer.cancel(true);
        }
        timer = executor.schedule(this::mainLoop, getDelay(), TimeUnit.MILLISECONDS);

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
                logger.logf("WON ELECTION[%d] WITH (%d/%d) VOTES\n", term, votesRecieved, servers.size() + 1);
                executor.execute(this::sendHeartBeats);
            } else {
                logger.logf("LOST ELECTION[%d] WITH (%d/%d) VOTES\n", term, votesRecieved, servers.size() + 1);
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
                    System.out.println(term+"\t"+req.getTerm());
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
            if (votedFor != null && !votedFor.equals(req.getId())) {
                return rejectVote(req, "ALREADY VOTING FOR:" + votedFor);
            }
            voteFor(req.getId());
            logger.logf("VOTED FOR[%d]: %s\n", req.getTerm(), req.getId());
            return new RequestVoteResponse(this.term, true);
        }
    }

    public RequestVoteResponse rejectVote(RequestVoteRequest x, String s) {
        logger.logf("REJECTED VOTE [%d] FOR [%s]:\t%s\n", term, x.getId().toString(), s);
        return new RequestVoteResponse(x.getTerm(), false);
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
                executor.execute(this::startElection);

            }
            case FOLLOWER -> {
                synchronized (lock) {
                    term++;
                    voteFor(this.id);
                    this.votesRecieved = 1;
                    changeState(RaftState.CANDIDATE);
                }
                executor.execute(this::startElection);
            }
        }
        resetTimers();

    }

    public Raft(Config config) throws IOException {
        this.id = config.id;
        this.config = config;
        this.servers = this.config.servers;
        manager = new RPCManagerTCP(this);
        logger.log("STARTING RAFT");
        try {
            executor = new ScheduledThreadPoolExecutor(5);
        } catch (IllegalArgumentException e) {
            //LOG FATAL N CANNOT BE LESSER THAN 0
            throw new RuntimeException("THREAD SIZE < 0");
        }
        changeState(RaftState.FOLLOWER);
        executor.execute(this::mainLoop);

//        executor.schedule(this::mainLoop, 150, TimeUnit.MILLISECONDS);
        executor.execute(manager);
    }

    public synchronized long getDelay() {
        switch (state) {
            case CANDIDATE, RaftState.FOLLOWER -> {
                return 450 + (int) (Math.random() * 150);
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
        if (state == RaftState.FOLLOWER) {
            synchronized (lock) {
                if (electionBid != null) {
                    electionBid.cancel(true);
                }
            }
        }
        int b = 0, h = 0;
        switch (state) {
            case FOLLOWER -> {
                logger.setFormat(AnsiColor.WHITE, b, h);
            }
            case CANDIDATE -> {
                logger.setFormat(AnsiColor.GREEN, b, h);
            }
            case LEADER -> {
                logger.setFormat(AnsiColor.BLUE, b, h);
            }
        }
        logger.log("CHANGED STATE TO:" + state);
        this.state = state;
        resetTimers();
    }

}
