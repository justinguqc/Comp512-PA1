# comp512 programming assignment 1

RMI distribution is implemented with central customer ownership at middleware and three
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
