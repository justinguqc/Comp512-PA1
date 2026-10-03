package Tests;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Stage 7: three TCP managers, async middleware, and the blocking console in separate JVMs. */
public final class ProcessTcpTest {
    private static boolean scripts;
    private static ProcessBuilder launch(String java, String cp, String role, String... args) {
        List<String> command;
        if (scripts) {
            boolean windows = System.getProperty("os.name").startsWith("Windows");
            String bash = System.getenv().getOrDefault("PA1_BASH", windows ? "C:/Program Files/Git/bin/bash.exe" : "/bin/bash");
            command = new ArrayList<>(List.of(bash));
            if (windows) command.addAll(List.of("-c", "export PATH=/usr/bin:$PATH; exec /usr/bin/bash \"$@\"", "bash-launcher"));
            command.add(switch (role) {
                case "Server.TCP.TCPResourceManager" -> "Template/Server/run_tcp_server.sh";
                case "Server.TCP.TCPMiddleware" -> "Template/Server/run_tcp_middleware.sh";
                default -> "Template/Client/run_tcp_client.sh";
            });
        } else command = new ArrayList<>(List.of(java, "-cp", cp, role));
        command.addAll(Arrays.asList(args));
        return new ProcessBuilder(command);
    }
    public static void main(String[] args) throws Exception {
        scripts = args.length > 0 && args[0].equals("scripts");
        Path logs = Files.createTempDirectory(Path.of("build"), "tcp-process-");
        List<Process> processes = new ArrayList<>();
        List<ServerSocket> reserved = new ArrayList<>(); int[] ports = new int[4];
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = Path.of(System.getProperty("java.class.path")).toAbsolutePath().toString();
        try {
            for (int i = 0; i < 4; i++) { ServerSocket socket = new ServerSocket(0); reserved.add(socket); ports[i] = socket.getLocalPort(); }
            for (ServerSocket socket : reserved) socket.close();
            for (int i = 0; i < 3; i++) {
                String name = List.of("Flights", "Cars", "Rooms").get(i);
                Path log = logs.resolve(name + ".log");
                Process process = launch(java, cp, "Server.TCP.TCPResourceManager", name, "" + ports[i])
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start(); processes.add(process); ready(process, log);
            }
            Path log = logs.resolve("Middleware.log");
            Process middleware = launch(java, cp, "Server.TCP.TCPMiddleware", "" + ports[3],
                    "localhost:" + ports[0], "localhost:" + ports[1], "localhost:" + ports[2])
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start(); processes.add(middleware); ready(middleware, log);
            Path output = logs.resolve("Client.log");
            Process client = launch(java, cp, "Client.TCPClient", "localhost", "" + ports[3])
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start(); processes.add(client);
            try (Writer input = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)) {
                input.write(String.join("\n", "Help", "AddFlight,512,3,100", "AddFlight,512,1,0", "AddCars,Montreal,2,30", "AddRooms,Montreal,2,80",
                        "AddCustomerID,700", "ReserveFlight,700,512", "ReserveCar,700,Montreal", "ReserveRoom,700,Montreal",
                        "QueryCustomer,700", "QueryFlightPrice,512", "QueryCarsPrice,Montreal", "QueryRoomsPrice,Montreal",
                        "DeleteFlight,512", "DeleteCars,Montreal", "DeleteRooms,Montreal", "Bundle,700,512,512,Montreal,1,1",
                        "QueryCustomer,700", "DeleteCustomer,700", "QueryFlight,512", "QueryCars,Montreal", "QueryRooms,Montreal",
                        "DeleteFlight,512", "DeleteCars,Montreal", "DeleteRooms,Montreal", "AddCustomer", "Quit", ""));
            }
            StarterTest.check(client.waitFor(15, TimeUnit.SECONDS), "blocking TCP console completes sequential commands");
            String text = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            StarterTest.check(client.exitValue() == 0 && !text.contains("exception:"), "TCP console succeeds: " + text);
            for (String expected : List.of("Total cost: $210", "Total cost: $520", "Bundle Reserved", "Customer Deleted",
                    "Number of seats available: 4", "Number of cars at this location: 2", "Number of rooms at this location: 2",
                    "Flight could not be deleted", "Cars could not be deleted", "Rooms could not be deleted",
                    "Flight Deleted", "Cars Deleted", "Rooms Deleted", "Add customer ID:"))
                StarterTest.check(text.contains(expected), "console output missing " + expected + "\n" + text);
            System.out.println("PASS ProcessTcpTest" + (scripts ? " via Bash launchers" : "") + " (logs: " + logs + ")");
        } finally {
            for (ServerSocket socket : reserved) socket.close(); Collections.reverse(processes);
            for (Process process : processes) {
                List<ProcessHandle> children = process.descendants().toList();
                children.forEach(ProcessHandle::destroy);
                process.destroy(); if (!process.waitFor(3, TimeUnit.SECONDS)) { process.destroyForcibly(); process.waitFor(3, TimeUnit.SECONDS); }
                for (ProcessHandle child : children) if (child.isAlive()) child.destroyForcibly();
            }
        }
    }
    private static void ready(Process process, Path log) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (process.isAlive() && System.nanoTime() < end) {
            if (new String(Files.readAllBytes(log), StandardCharsets.UTF_8).contains(" ready on TCP ")) return;
            Thread.sleep(25);
        }
        throw new AssertionError("TCP startup failed: " + new String(Files.readAllBytes(log), StandardCharsets.UTF_8));
    }
}
