package RAFT.RAFT;

import Logging.AnsiColor;
import Logging.AppendOnlyLog;
import Logging.RaftLogger;
import RAFT.RAFT.Logs.Log;
import RAFT.RPC.*;
import RAFT.RPC.TCPSocket.RPCManagerTCP;
import RAFT.RPC.Type.*;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    private volatile long votesReceived = 0;
    ScheduledThreadPoolExecutor executor;
    List<RPCServer> servers;
    RPCManagerTCP manager;

    List<Log> logs;
    private ID leaderID;
    long commitIndex = -1;
    AppendOnlyLog aol;

    private volatile ScheduledFuture<?> timer = null;


    public void sendHeartBeats() {

        List<Future<Long>> futures = new ArrayList<>();
        for (var x : servers) {
            //SEND HEARTBEAT
            HeartBeatRequest request = new HeartBeatRequest();
            request.setLeaderCommit(commitIndex);
            request.setLeaderID(id);
            request.setLeaderTerm(term);
            request.setPrevLogIndex(x.getLogIndex());
            request.setPrevLogTerm(0);
            request.setLogs(logs.subList((int) Math.max(x.getLogIndex(), 0), logs.size()));
            var f = executor.submit(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    var r = x.sendHeartBeat(request);
                    if (r.isEmpty()) return Long.MAX_VALUE;
                    ;
                    var reply = r.get();
                    synchronized (lock) {
                        if (reply.term > term) {
                            changeState(RaftState.FOLLOWER);
                            futures.forEach(future -> future.cancel(false));
                            return Long.MAX_VALUE;
                        }
                        if (!reply.success) {
                            x.setLogIndex(Math.max(x.getLogIndex() - 1, -1));
//                        logger.log("CANNOT APPEND TO:" + x.getId() + "\tTRYING WITH INDEX:" + x.getLogIndex());

                        } else {
                            x.setLogIndex(logs.size());
                            return x.getLogIndex();
                        }
                    }
                    return Long.MAX_VALUE;
                }
            });
            futures.add(f);

        }

        var min = new AtomicLong(Long.MAX_VALUE);
        var consenus = new AtomicLong(0);
        for (var x : futures) {
            executor.submit(() -> {
                try {
                    min.set(Math.min(min.get(), x.get()));
                    consenus.incrementAndGet();
                    if (isMajority(consenus.get())) {
                        if (min.get() != commitIndex && min.get() != Long.MAX_VALUE)
                            commit(min.get());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    new RuntimeException("NEED TO RESTORE LOGS").printStackTrace();
                }

            });
        }

    }

    @Override
    public @NonNull HeartBeatResponse recieveHeartBeat(HeartBeatRequest req) {
        resetTimers();
        synchronized (lock) {
            if (term <= req.getLeaderTerm()) {
                if (state != RaftState.FOLLOWER) {
                    changeState(RaftState.FOLLOWER);
                }
                if (term != req.getLeaderTerm()) {
                    logger.logf("UPDATED TERM TO [%d] <- [%d]\n", req.getLeaderTerm(), term);
                }
                term = req.getLeaderTerm();
            }
            if (term > req.getLeaderTerm()) {
                return new HeartBeatResponse(this.term, false);
            }
            leaderID = req.getLeaderID();
            //RESET TIMER

            voteFor(null);
            //ACCEPT LOGS
            if (req.getPrevLogIndex() < 0) {

            } else if (req.getPrevLogIndex() > logs.size()) {
                return new HeartBeatResponse(this.term, false);
            }
            logs.addAll(req.getLogs());
            if (!req.getLogs().isEmpty()) {
                logger.log(req.getLogs().toString());
            }
            if (req.getLeaderCommit() != commitIndex) {
                try {
                    commit(req.getLeaderCommit());
                } catch (IOException e) {
                    return null;
                }
            }

            return new HeartBeatResponse(this.term, true);
        }
    }

    public void startElection() {
        long electionStartTerm = this.term;
        List<Future<?>> futures = new ArrayList<>();
        Phaser phaser = new Phaser(1);
        for (var x : servers) {
            //SEND REQUEST VOTE
            RequestVoteRequest request = new RequestVoteRequest();
            request.setId(id);
            request.setTerm(term);
            request.setLastLogIndex(commitIndex);
            request.setLastLogTerm(0);

            var f = executor.submit(() -> {
                var r = x.requestVote(request);
                phaser.arrive();
                if (r.isEmpty()) return;

                var reply = r.get();
                synchronized (lock) {
                    if (reply.getTerm() > term) {
                        changeState(RaftState.FOLLOWER);
                        futures.forEach(future -> future.cancel(false));
                        phaser.forceTermination();
                    }
                    if (reply.isVoteGranted()) {
                        this.votesReceived++;
                    }
                    if (isMajority()) {
                        changeState(RaftState.LEADER);
                        futures.forEach(future -> future.cancel(false));
                        phaser.forceTermination();
                    }

                }
            });
            futures.add(f);
            phaser.register();
        }
        phaser.arriveAndAwaitAdvance();
        synchronized (lock) {
            if (state == RaftState.LEADER) {
                logger.logf("WON ELECTION[%d] WITH (%d/%d) VOTES\n", electionStartTerm, votesReceived, servers.size() + 1);
                executor.execute(this::sendHeartBeats);

            } else {
                logger.logf("LOST ELECTION[%d] WITH (%d/%d) VOTES\n", electionStartTerm, votesReceived, servers.size() + 1);

            }
        }

    }

    @Override
    public synchronized @NonNull RequestVoteResponse recieveRequestVote(RequestVoteRequest req) {

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
            if (votedFor != null && !votedFor.equals(req.getId())) {
                return rejectVote(req, "ALREADY VOTING FOR:" + votedFor);
            }
            voteFor(req.getId());
            logger.logf("VOTED FOR[%d]: %s\n", req.getTerm(), req.getId());
            return new RequestVoteResponse(this.term, true);
        }
    }

    @Override
    public UpdateResponse update(RPCString string) {
        System.out.println(string);
        synchronized (lock) {
            if (state != RaftState.LEADER)
                return new UpdateResponse(leaderID);
            Log l = new Log(logs.size(), term, string);
            logs.add(l);
            return new UpdateResponse(l);
        }
    }


    private void mainLoop() {
        try {
            switch (state) {
                case LEADER -> {
                    sendHeartBeats();
                }
                case CANDIDATE -> {
                    synchronized (lock) {
                        term++;
                        voteFor(this.id);
                        this.votesReceived = 1;
                    }
                    executor.execute(this::startElection);

                }
                case FOLLOWER -> {
                    synchronized (lock) {
                        term++;
                        voteFor(this.id);
                        this.votesReceived = 1;
                        changeState(RaftState.CANDIDATE);
                    }
                    executor.execute(this::startElection);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetTimers();

    }

    public Raft(Config config) throws IOException {
        this.id = config.id;
        this.config = config;
        this.servers = this.config.servers;
        manager = new RPCManagerTCP(this, executor);
        logger.log("STARTING RAFT");
        try {
            executor = new ScheduledThreadPoolExecutor(5);
        } catch (IllegalArgumentException e) {
            //LOG FATAL N CANNOT BE LESSER THAN 0
            throw new RuntimeException("THREAD SIZE < 0");
        }
        aol = new AppendOnlyLog(config.logfile.toString());
//        logs = aol.loadInitial();
        logs=new ArrayList<>();
        logger.log("LOADED LOGS:\t" + logs.size());
        this.commitIndex = logs.size() ;

//        aol.writeLog(new Log(10,10,"Hello World"));

        executor.execute(this::mainLoop);

//        executor.schedule(this::mainLoop, 150, TimeUnit.MILLISECONDS);
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
        if (id == null) {
        } else if (id == this.id) {
            System.out.println("VOTING FOR MYSELF");
        } else {
            System.out.println("VOTING FOR:" + id);
        }
        this.votedFor = id;
    }

    public synchronized void changeState(RaftState state) {
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
        if (state == RaftState.LEADER) {
            for (var x : servers) {
                x.setLogIndex(this.logs.size());
            }
        }
        resetTimers();
    }

    private void commit(Log log) {
        System.out.println(log);
    }

    private synchronized void commit(long index) throws IOException {
        synchronized (lock) {
            commitIndex = index;
            logger.log("COMMITING TO INDEX:" + index);
            for (Log l : logs) {
                aol.writeLog(l);
            }
        }
    }


    public synchronized boolean isMajority() {
        return isMajority(votesReceived);
    }

    public synchronized boolean isMajority(long val) {
        return (2 * val > (this.servers.size() + 1));
    }


    synchronized void resetTimers() {
        //RESET TIMER
        if (timer != null) {
            timer.cancel(true);
        }
        timer = executor.schedule(this::mainLoop, getDelay(), TimeUnit.MILLISECONDS);

    }

    public RequestVoteResponse rejectVote(RequestVoteRequest x, String s) {
        logger.logf("REJECTED VOTE [%d] FOR [%s]:\t%s\n", term, x.getId().toString(), s);
        return new RequestVoteResponse(x.getTerm(), false);
    }


}
