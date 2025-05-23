package RAFT.RAFT;

import Logging.AnsiColor;
import Logging.AppendOnlyLog;
import Logging.RaftLogger;
import RAFT.ClientUpdate;
import RAFT.RAFT.Logs.Log;
import RAFT.RAFT.RPCType.*;
import RAFT.RPC.*;
import RAFT.RPC.TCPSocket.RPCManagerTCP;
import RAFT.RPC.Type.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
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
    Optional<ClientUpdate> clientUpdate = Optional.empty();

    public void setClientUpdate(ClientUpdate update) {
        clientUpdate = Optional.of(update);
    }

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
            request.setLogs(logs.subList(Math.min((int) x.getLogIndex() + 1, logs.size()), logs.size()));
            if (x.getLogIndex() >= 0 && x.getLogIndex() < logs.size()) {
                var l = logs.get((int) x.getLogIndex());
//                System.out.println(l);
                request.setPrevLogTerm(l.getTerm());
            } else {
                request.setPrevLogTerm(-1);
            }
            Optional<HeartBeatResponse> r;
            try {
                r = x.sendHeartBeat(request);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            var f = executor.submit(new Callable<Long>() {
                @Override
                public Long call() throws Exception {

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
                            logger.log("CANNOT APPEND TO:" + x.getId() + "\tTRYING WITH INDEX:" + x.getLogIndex());

                        } else {
//                            logger.log("HEARTBEAT");
                            x.setLogIndex(logs.size() - 1);
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
                        if (min.get() != commitIndex && min.get() != Long.MAX_VALUE) {
                            System.out.println(min.get() + "\t" + commitIndex);
                            commit(min.get());
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        }

    }

    @Override
    public @NonNull HeartBeatResponse receiveHeartBeat(HeartBeatRequest req) {
//        System.out.println(req.getPrevLogIndex());
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
                //NO LOGS
            } else {
                if (req.getPrevLogIndex() >= logs.size()) {
                    logger.log("REJECTING HEARTBEAT FOR LOGS:" + req.getPrevLogIndex());
                    return new HeartBeatResponse(this.term, false);
                } else if (logs.get((int) req.getPrevLogIndex()).getTerm() != req.getPrevLogTerm()) {
                    logger.logf("MISMATCH TERM IN LOG:%s ASKING LEADER TO REVERT", logs.get((int) req.getPrevLogTerm()));
                    return new HeartBeatResponse(this.term, false);
                }
            }
//            logs=logs.subList(0,(int)req.getLeaderCommit()+1);
            logs.addAll(req.getLogs());
            if (!req.getLogs().isEmpty()) {
                logger.log(req.getLogs().toString());
            }
//            logger.log(logs.toString());
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
            Optional<RequestVoteResponse> r;
            try {
                r = x.requestVote(request);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            var f = executor.submit(() -> {
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
    public synchronized @NonNull RequestVoteResponse receiveRequestVote(RequestVoteRequest req) {

        synchronized (lock) {
            //LOG STUFF

            if (commitIndex > req.getLastLogIndex()) {
                //MATCH TERM???(HAVE TO CHECK VALIDITY FOR THIS)
                if (term != req.getTerm()) {
                    logger.logf("UPDATED TERM TO [%d] <- [%d]\n", req.getTerm(), term);
                    term = req.getTerm();
                }

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
            return new UpdateResponse(l, this.id);
        }
    }

    @Override
    public RaftStatus status() {
        RaftStatus status = new RaftStatus();
        status.setId(this.getId());
        status.setLogs(logs);
        status.setTerm(this.term);
        status.setState(new RPCString(this.state.toString()));
        status.setCommited(commitIndex);
        return status;
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
        logs = aol.loadInitial();
//        logs=new ArrayList<>();
        logger.log("LOADED LOGS:\t" + logs.size());
        this.commitIndex = logs.size() - 1;

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


    private synchronized void commit(long index) throws IOException {
        synchronized (lock) {
            logger.log("COMMITING TO INDEX:" + index);
            for (long i = commitIndex + 1; i <= index; i++) {
                Log l = logs.get((int) i);
                aol.writeLog(logs.get((int) i));
                clientUpdate.ifPresent(x -> x.update(l));
            }
            commitIndex = index;
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
