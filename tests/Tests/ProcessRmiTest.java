package Tests;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** RMI acceptance: five separate JVMs, four registries, and the supplied console dispatcher. */
public final class ProcessRmiTest {
    private static final List<Process> processes = new ArrayList<>();
    private static final String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    private static final String classpath = Path.of(System.getProperty("java.class.path")).toAbsolutePath().toString();
    private static boolean scripts;

    public static void main(String[] args) throws Exception {
        scripts = args.length > 0 && args[0].equals("scripts");
        Path logs = Files.createTempDirectory(Path.of("build"), "rmi-process-");
        List<ServerSocket> reserved = new ArrayList<>();
        int[] ports = new int[4];
        try {
            for (int i = 0; i < ports.length; i++) {
                ServerSocket socket = new ServerSocket(0);
                reserved.add(socket);
                ports[i] = socket.getLocalPort();
            }
            for (ServerSocket socket : reserved) socket.close();
            String[] names = {"Flights", "Cars", "Rooms"};
            for (int i = 0; i < 3; i++) {
                Path log = logs.resolve(names[i] + ".log");
                Process backend = start(log, "Server.RMI.RMIResourceManager", names[i], String.valueOf(ports[i]), "process_");
                awaitReady(backend, log);
            }
            Path middlewareLog = logs.resolve("Middleware.log");
            Process middleware = start(middlewareLog, "Server.RMI.RMIMiddleware", "localhost:" + ports[0],
                    "localhost:" + ports[1], "localhost:" + ports[2], String.valueOf(ports[3]), "process_");
            awaitReady(middleware, middlewareLog);
            Path clientLog = logs.resolve("Client.log");
            Process client = start(clientLog, "-Dcomp512.rmi.port=" + ports[3], "-Dcomp512.rmi.prefix=process_",
                    "Client.RMIClient", "localhost", "Middleware");
            try (Writer input = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)) {
                input.write(String.join("\n", "Help", "AddFlight,512,2,100", "AddCars,Montreal,2,30", "AddRooms,Montreal,2,80",
                        "AddCustomerID,700", "ReserveFlight,700,512", "ReserveCar,700,Montreal", "ReserveRoom,700,Montreal",
                        "QueryCustomer,700", "QueryFlight,512", "QueryCars,Montreal", "QueryRooms,Montreal",
                        "QueryFlightPrice,512", "QueryCarsPrice,Montreal", "QueryRoomsPrice,Montreal",
                        "DeleteFlight,512", "DeleteCars,Montreal", "DeleteRooms,Montreal",
                        "Bundle,700,512,Montreal,1,1", "QueryCustomer,700", "DeleteCustomer,700",
                        "QueryFlight,512", "QueryCars,Montreal", "QueryRooms,Montreal", "DeleteFlight,512",
                        "DeleteCars,Montreal", "DeleteRooms,Montreal", "AddCustomer", "Quit", ""));
            }
            if (!client.waitFor(15, TimeUnit.SECONDS))
                throw new AssertionError("Console did not finish; output:\n" + Files.readString(clientLog));
            String output = Files.readString(clientLog);
            StarterTest.check(client.exitValue() == 0 && !output.contains("exception:"), "console exits without exceptions: " + output);
            for (String expected : List.of("Total cost: $210", "Total cost: $420", "Bundle Reserved", "Customer Deleted",
                    "Number of seats available: 2", "Number of cars at this location: 2", "Number of rooms at this location: 2",
                    "Flight could not be deleted", "Cars could not be deleted", "Rooms could not be deleted",
                    "Flight Deleted", "Cars Deleted", "Rooms Deleted", "Add customer ID:"))
                StarterTest.check(output.contains(expected), "console result missing: " + expected + "\n" + output);
            System.out.println("PASS ProcessRmiTest" + (scripts ? " via Bash launchers" : "") + " (logs: " + logs + ")");
        } finally {
            for (ServerSocket socket : reserved) socket.close();
            Collections.reverse(processes);
            for (Process process : processes) {
                List<ProcessHandle> children = process.descendants().toList();
                for (ProcessHandle child : children) child.destroy();
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(3, TimeUnit.SECONDS);
                }
                for (ProcessHandle child : children) if (child.isAlive()) child.destroyForcibly();
            }
        }
    }

    private static Process start(Path log, String... args) throws IOException {
        List<String> command = new ArrayList<>(List.of(java, "-Djava.rmi.server.hostname=127.0.0.1", "-cp", classpath));
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder;
        if (scripts) {
            boolean windows = System.getProperty("os.name").startsWith("Windows");
            String bash = System.getenv().getOrDefault("PA1_BASH", windows ? "C:/Program Files/Git/bin/bash.exe" : "/bin/bash");
            command = new ArrayList<>(List.of(bash));
            // Direct Git Bash execution does not initialize its Unix utility PATH.
            if (windows) command.addAll(List.of("-c", "export PATH=/usr/bin:$PATH; exec /usr/bin/bash \"$@\"", "bash-launcher"));
            if (args[0].startsWith("-D")) {
                command.add("Template/Client/run_client.sh");
                command.addAll(List.of("localhost", "Middleware"));
            } else {
                command.add(args[0].endsWith("RMIMiddleware") ? "Template/Server/run_middleware.sh" : "Template/Server/run_server.sh");
                command.addAll(Arrays.asList(args).subList(1, args.length));
            }
            builder = new ProcessBuilder(command);
            builder.environment().put("RMI_HOSTNAME", "127.0.0.1");
            if (args[0].startsWith("-D")) {
                builder.environment().put("RMI_PORT", args[0].split("=", 2)[1]);
                builder.environment().put("RMI_PREFIX", args[1].split("=", 2)[1]);
            }
        } else builder = new ProcessBuilder(command);
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        processes.add(process);
        return process;
    }

    private static void awaitReady(Process process, Path log) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline && process.isAlive()) {
            if (Files.readString(log).contains(" ready on registry ")) return;
            Thread.sleep(25); // Readiness polling only; concurrency assertions use latches.
        }
        throw new AssertionError("Server startup failed: " + Files.readString(log));
    }
}
