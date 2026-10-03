# Running the RMI and TCP distributions

Requires JDK 17 or newer. No external Java libraries, Maven, or Gradle are needed.
The original client-facing `IResourceManager` interface and console commands are unchanged.
The small console fixes are documented in `CHANGELOG.md`.

## Build on Linux / lab machines

From the repository root:

```bash
make -C Template/Server
make -C Template/Client
bash tests/run-tests.sh
```

The launch scripts resolve their working directory themselves. Invoke them with `bash`;
executable permission is not required. The RMI hosts create or join their registry themselves,
so do not start the legacy `run_rmi.sh` separately. An external registry is optional, but it
must have the server classes available for backend interface unmarshalling.

## Local topology

Run these in four terminals, waiting for each backend's ready message before middleware:

```bash
bash Template/Server/run_server.sh Flights
bash Template/Server/run_server.sh Cars
bash Template/Server/run_server.sh Rooms
bash Template/Server/run_middleware.sh localhost localhost localhost
```

The three managers and middleware can share registry 1099 on one machine because they have
different bindings. The first host owns that local registry: keep it running until the other
hosts stop. Start the client in a fifth terminal:

```bash
bash Template/Client/run_client.sh localhost Middleware
```

If ports conflict or you want fully independent local registries:

```bash
bash Template/Server/run_server.sh Flights 3101 group_demo_
bash Template/Server/run_server.sh Cars 3102 group_demo_
bash Template/Server/run_server.sh Rooms 3103 group_demo_
bash Template/Server/run_middleware.sh localhost:3101 localhost:3102 localhost:3103 3104 group_demo_
RMI_PORT=3104 RMI_PREFIX=group_demo_ bash Template/Client/run_client.sh localhost Middleware
```

`group_demo_` is an example prefix; use a unique group prefix on shared lab hosts. Both sides
of each RMI lookup must use the same prefix. Java entry points default to `group_xx_` and 1099
for compatibility with the starter, not as a recommended shared-host configuration.

## Native Windows / PowerShell

Run the test suite from the repository root:

```powershell
./tests/run-tests.ps1
```

It compiles every source into `build/tests`. Start the four server roles in separate terminals:

```powershell
java -cp build/tests Server.RMI.RMIResourceManager Flights 3101 group_demo_
java -cp build/tests Server.RMI.RMIResourceManager Cars 3102 group_demo_
java -cp build/tests Server.RMI.RMIResourceManager Rooms 3103 group_demo_
java -cp build/tests Server.RMI.RMIMiddleware localhost:3101 localhost:3102 localhost:3103 3104 group_demo_
```

After all servers are ready, run the original console:

```powershell
java '-Dcomp512.rmi.port=3104' '-Dcomp512.rmi.prefix=group_demo_' -cp build/tests Client.RMIClient localhost Middleware
```

## Five-machine demo

Use one distinct host for each role: client, middleware, Flights, Cars, Rooms. Replace the
example hostnames below with actual available lab machines; those machines have not been
selected or tested from this workspace. Compile the code on each host or use a shared checkout.

On the Flights host:

```bash
RMI_HOSTNAME=flights-host bash Template/Server/run_server.sh Flights 3101 group_demo_ 4101
```

On the Cars and Rooms hosts:

```bash
RMI_HOSTNAME=cars-host bash Template/Server/run_server.sh Cars 3102 group_demo_ 4102
RMI_HOSTNAME=rooms-host bash Template/Server/run_server.sh Rooms 3103 group_demo_ 4103
```

On the middleware host, after all three backends report ready:

```bash
RMI_HOSTNAME=middleware-host bash Template/Server/run_middleware.sh flights-host:3101 cars-host:3102 rooms-host:3103 3104 group_demo_ 4104
```

On the fifth host:

```bash
RMI_PORT=3104 RMI_PREFIX=group_demo_ bash Template/Client/run_client.sh middleware-host Middleware
```

The advertised hostnames must resolve from the calling machines. Both registry ports
(3101-3104 here) and exported object ports (4101-4104 here) must be reachable. Object port 0
uses an ephemeral port; fixed ports are useful on hosts with port restrictions.
`RMI_HOSTNAME` maps to Java's `java.rmi.server.hostname` property in the server scripts.

The optional `run_servers.sh` tmux/SSH helper accepts four hostnames for the server roles,
uses default settings, and assumes identical shared server-directory paths. Manual startup
above is preferable for nondefault ports and prefixes. If middleware starts before all
bindings exist, restart it after the three backend ready messages.

## Example console sequence

```text
AddFlight,512,3,100
AddCars,Montreal,2,30
AddRooms,Montreal,2,80
AddCustomerID,700
Bundle,700,512,512,Montreal,1,1
QueryCustomer,700
QueryFlight,512
DeleteFlight,512
DeleteCustomer,700
QueryFlight,512
DeleteFlight,512
Quit
```

Expected: the bill contains two flight seats, one car, one room, and total $310. One flight
seat remains. Flight deletion initially fails. Customer deletion restores three seats;
flight deletion then succeeds. Bundle flags accept 0/1, Y/N, or true/false.

## Shutdown, failures, and limitations

- Stop client, middleware, then backend processes with Ctrl+C. Hosts unbind their services.
  For a shared local registry, stop its owning first backend last.
- If middleware startup reports a missing binding or connection refusal, check backend
  readiness, hostname/port, prefix, and advertised RMI hostname.
- Middleware owns customers; backends retain inventory and booking tokens only for distributed
  requests. The original backend customer API still exists for starter compatibility but
  middleware never invokes it.
- Customers, inventory, booking IDs, and cancellation tombstones are in memory. Restarting
  a component loses its state; restart the entire topology for a fresh demo. There is no
  durable recovery, replication, or transaction coordinator.
- A failed bundle returns false after confirmed compensation. An unreachable backend produces
  a remote error and retains incomplete cancellation; retry a reservation or delete that
  customer after the same backend endpoint recovers. New bookings are rejected during an
  incomplete customer deletion; retry `DeleteCustomer` to finish it. No server automatically
  reconnects a stale RMI stub to a restarted backend.
- Inventory queries may observe temporary bundle acquisitions. Bills preserve the starter's
  grouped latest-price convention, including repricing earlier reservations of the same item
  when a later booking uses a new price. This is documented behavior, not historical billing.
- The supplied console retains its RMI connection retry behavior. Client-to-middleware lost
  replies can still have an uncertain outcome; backend token idempotency does not provide
  end-to-end exactly-once console commands.
- The RMI snapshot reviewed with the partner is preserved as annotated Git tag `rmi-complete`.

## TCP topology and launch commands

Build with the same Make targets above, or run `./tests/run-tests.ps1` on Windows.
TCP uses ordinary sockets on both links and does not need an RMI registry. Run these
in four terminals, starting all managers before middleware:

```bash
bash Template/Server/run_tcp_server.sh Flights 4001
bash Template/Server/run_tcp_server.sh Cars 4002
bash Template/Server/run_tcp_server.sh Rooms 4003
bash Template/Server/run_tcp_middleware.sh 4004 localhost:4001 localhost:4002 localhost:4003
```

In the fifth terminal:

```bash
bash Template/Client/run_tcp_client.sh localhost 4004
```

For native PowerShell, use the classes built by the test runner:

```powershell
java -cp build/tests Server.TCP.TCPResourceManager Flights 4001
java -cp build/tests Server.TCP.TCPResourceManager Cars 4002
java -cp build/tests Server.TCP.TCPResourceManager Rooms 4003
java -cp build/tests Server.TCP.TCPMiddleware 4004 localhost:4001 localhost:4002 localhost:4003
java -cp build/tests Client.TCPClient localhost 4004
```

For five physical machines, run each manager command on its own host. On the middleware
host replace the three localhost endpoints with `flights-host:4001`, `cars-host:4002`,
and `rooms-host:4003`; on the client host use `middleware-host 4004`. Those names are
placeholders. DNS and each receiving TCP port must be reachable. The example console
sequence above works unchanged. Stop client, middleware, then managers with Ctrl+C.
TCP scripts resolve their directory; retain both Template/Client and Template/Server
for the client classpath. A Make build compiles both transports.

The optional last middleware/client argument is a positive timeout in milliseconds
(default 30000). Backend connect and reply waits use that timeout; it is not a deadline
for an entire multi-resource bundle. The console waits for its own command. Middleware
accept/read/dispatch loops compose futures without waiting for inventory replies; dedicated
socket writers handle queued output. Requests from one customer are ordered by future
chains; other customers and inventory queries can progress independently.

Limits: 1 MiB per frame, 10000 flight strings, 64 accepted connections per server, 1024
pending requests per connection/channel/customer, and 256 queued tasks for each inventory
server's four workers. Overload is rejected or closes the affected connection. Delayed
responses correlate by request ID and may arrive out of order.

A timeout or disconnect fails all pending requests and closes that channel. A mutation may
already have occurred; the console does not automatically resend it. There is no automatic
TCP reconnect. A reported remote operation error while the channel remains alive allows
retained compensation/deletion to be retried. A closed backend channel needs operational
recovery; inspect uncertain outcomes and restart the topology for a fresh demo. All state
remains in memory, and a component restart does not provide durable recovery.

## Calling the general TCP wrapping service

Client code can reuse the same service as the console, without building messages per method:

```java
try (TcpChannel channel = new TcpChannel("middleware-host", 4004, 30000)) {
    IResourceManager client = Services.blocking(IResourceManager.class, channel);
    client.addFlight(512, 3, 100);
    int seats = client.queryFlight(512);
}
```

Import `Server.TCP.TcpChannel`, `Server.Common.Services`, and
`Server.Interface.IResourceManager`. `Services.blocking` supplies the blocking interface;
`TcpChannel.invoke(Method, Object[])` supplies the reusable asynchronous invocation service.
Both use `Protocol` to encode/decode the same versioned request/response envelopes, and
`Operations` validates interface-derived signatures. Middleware exposes client operations;
internal reserve/release operations are accepted only by inventory endpoints. The interface's
`RemoteException` type is reused for local error reporting; TCP performs no RMI network calls.
