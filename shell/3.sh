tmux split-window -h -l 66%
tmux split-window -h

tmux select-pane -t 1
tmux send-keys "java -jar out/artifacts/RAFT/RAFT.jar res/config.json 1" C-m

tmux select-pane -t 2
tmux send-keys "java -jar out/artifacts/RAFT/RAFT.jar res/config.json 2" C-m

tmux select-pane -t 3
tmux send-keys "java -jar out/artifacts/RAFT/RAFT.jar res/config.json 3" C-m
