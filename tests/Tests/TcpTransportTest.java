package Tests;

import Server.Common.ResourceManager;
import Server.Common.Services;
import Server.Interface.IInventoryManager;
import Server.TCP.TcpChannel;
import Server.TCP.TcpServer;

public final class TcpTransportTest {
    public static void main(String[] args) throws Exception {
        try (TcpServer server = TcpServer.inventory(0, new ResourceManager("Flights"));
             TcpChannel channel = new TcpChannel("localhost", server.port(), 5000)) {
            IInventoryManager client = Services.blocking(IInventoryManager.class, channel);
            StarterTest.check(client.addFlight(512, 1, 100) && client.queryFlight(512) == 1,
                    "general TCP invocation handles add and query");
            StarterTest.check(client.reserveInventory("flight-512", 1, "booking").getPrice() == 100
                    && client.releaseInventory("booking") && client.queryFlight(512) == 1,
                    "backend TCP calls preserve atomic receipt/release behavior");
        }
        System.out.println("PASS TcpTransportTest");
    }
}
