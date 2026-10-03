# Five-machine TCP test record

Date: October 3, 2026 (Montreal local date). Results were confirmed by the user
during the guided deployment; the agent did not access the remote hosts or capture logs.

| Role | Host | TCP port |
| --- | --- | --- |
| Client | mimi.cs.mcgill.ca | Outbound connection |
| Middleware | tr-open-01.cs.mcgill.ca | 4004 |
| Flights | open-gpu-1.cs.mcgill.ca | 4001 |
| Cars | lab1-1.cs.mcgill.ca | 4002 |
| Rooms | tr-open-28.cs.mcgill.ca | 4003 |

All five hosts were reached through SSH, Java versions were checked, and the shared
checkout was compiled targeting Java 17. The three managers and middleware reported
ready; the client connected and displayed Help.

## Confirmed scenarios

- Add three flight-512 seats at $100, two Montreal cars at $30, and two rooms at $80.
  Customer 700 books two copies of flight 512 plus a car and room: bill $310,
  one seat/car/room remains.
- Flight deletion fails while booked. Deleting customer 700 restores three seats,
  two cars, and two rooms; flight deletion then succeeds.
- Flight 513 has two seats; SoldOut has zero cars. Customer 701's flight/car bundle
  fails; both flight seats remain and the bill is $0.
- Two clients run on mimi. Prepare flight 514 and customer 702. Suspend the Flights
  Java process with SIGSTOP and arrange SIGCONT after 20 seconds. Client A's flight
  reservation waits; client B's Montreal Cars query returns two while A waits.
  After Flights resumes, A's reservation succeeds. The user confirmed both observations.

The user ended TCP testing after these scenarios. No five-host timeout/disconnect,
large-scale contention, or performance result is claimed. RMI tests previously passed
locally, but RMI five-machine testing is still pending. No raw remote logs were supplied.
