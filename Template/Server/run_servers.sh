#!/bin/bash 

# RMI deployment convenience: supply Flights, Cars, Rooms, and Middleware hosts.
# Requires tmux, SSH, identical shared paths, and default registry/binding settings.
if [[ $# -ne 4 ]]; then
    echo "Usage: $0 Flights-host Cars-host Rooms-host Middleware-host" >&2
    exit 1
fi
MACHINES=("$@")

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd '$(pwd)' > /dev/null; echo -n 'Connected to '; hostname; bash run_server.sh Flights\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd '$(pwd)' > /dev/null; echo -n 'Connected to '; hostname; bash run_server.sh Cars\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd '$(pwd)' > /dev/null; echo -n 'Connected to '; hostname; bash run_server.sh Rooms\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd '$(pwd)' > /dev/null; echo -n 'Connected to '; hostname; bash run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]}\"" C-m \;
