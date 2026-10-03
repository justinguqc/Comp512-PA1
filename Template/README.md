# comp512 programming assignment 1

RMI and TCP distributions are implemented with central customer ownership at middleware and three
inventory managers. See [`../docs/RUNNING.md`](../docs/RUNNING.md) for local/five-host launch
commands and [`../docs/TESTING.md`](../docs/TESTING.md) for the offline TDD suite.
Start the backends before middleware. The hosts manage their own registries.

To run the RMI resource manager:

```
cd Server/
bash run_server.sh Flights # repeat for Cars and Rooms in other terminals
bash run_middleware.sh <Flights-host> <Cars-host> <Rooms-host>
bash run_servers.sh <Flights-host> <Cars-host> <Rooms-host> <Middleware-host> # optional tmux/SSH helper
```

To run the RMI client:

```
cd Client
bash run_client.sh <middleware-host> Middleware
```

To run TCP, build with `make -C Server` and `make -C Client` from this directory,
then run each line in a separate terminal (managers first):

```bash
bash Server/run_tcp_server.sh Flights 4001
bash Server/run_tcp_server.sh Cars 4002
bash Server/run_tcp_server.sh Rooms 4003
bash Server/run_tcp_middleware.sh 4004 localhost:4001 localhost:4002 localhost:4003
bash Client/run_tcp_client.sh localhost 4004
```

The client blocks per command; middleware uses asynchronous backend calls and per-customer
future chains. A general invocation/protocol service wraps every interface operation.
The accepted RMI solution is preserved under Git tag `rmi-complete`.
