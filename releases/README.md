# Standalone RMI snapshot

`rmi-complete.zip` is exported from the accepted `rmi-complete` Git tag, commit `6c17981`.
It contains the RMI-only source tree, eight test suites, launch scripts, Makefiles, and
technical documentation from that snapshot. It contains no TCP source or build output.
The existing working tree continues to contain the final RMI/TCP implementation.

On a Linux deployment machine, from the shared Git checkout:

```bash
git pull --ff-only
unzip releases/rmi-complete.zip -d ../Comp512-PA1-RMI
cd ../Comp512-PA1-RMI
export JDK_JAVAC_OPTIONS="--release 17"
make -C Template/Server
make -C Template/Client
bash tests/run-tests.sh
```

Extract into a fresh directory; a shared home needs extraction and compilation only once.
The archive's `docs/RUNNING.md` contains startup commands. For the chosen five hosts,
run each command on its corresponding machine, from `Comp512-PA1-RMI`:

```bash
# Flights: open-gpu-1.cs.mcgill.ca
RMI_HOSTNAME=open-gpu-1.cs.mcgill.ca bash Template/Server/run_server.sh Flights 3101 group_30_ 4101

# Cars: lab1-1.cs.mcgill.ca
RMI_HOSTNAME=lab1-1.cs.mcgill.ca bash Template/Server/run_server.sh Cars 3102 group_30_ 4102

# Rooms: tr-open-28.cs.mcgill.ca
RMI_HOSTNAME=tr-open-28.cs.mcgill.ca bash Template/Server/run_server.sh Rooms 3103 group_30_ 4103

# Middleware: tr-open-01.cs.mcgill.ca; start after all three managers are ready.
RMI_HOSTNAME=tr-open-01.cs.mcgill.ca bash Template/Server/run_middleware.sh open-gpu-1.cs.mcgill.ca:3101 lab1-1.cs.mcgill.ca:3102 tr-open-28.cs.mcgill.ca:3103 3104 group_30_ 4104

# Client: mimi.cs.mcgill.ca
RMI_PORT=3104 RMI_PREFIX=group_30_ bash Template/Client/run_client.sh tr-open-01.cs.mcgill.ca Middleware
```

The hosts manage their registries; no separate `rmiregistry` command is required.
Registry ports 3101–3104 and object ports 4101–4104 must be reachable. RMI five-host
testing has not yet been performed. This is the accepted AI-assisted RMI version,
not Yinkun's independent non-AI preliminary version.
