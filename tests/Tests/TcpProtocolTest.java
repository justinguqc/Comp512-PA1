package Tests;

import Server.TCP.Protocol;
import Server.TCP.Operations;
import java.io.*;
import java.util.Vector;
import java.util.List;

public final class TcpProtocolTest {
    public static void main(String[] args) throws Exception {
        var method = Operations.method("bundle", int.class, Vector.class, String.class, boolean.class, boolean.class);
        var request = new Protocol.Request(42, Operations.key(method), new Object[]{7, new Vector<>(List.of("512", "513")), "Montréal\nToronto", true, false});
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Protocol.write(output, request);
        var decoded = (Protocol.Request) Protocol.read(new ByteArrayInputStream(output.toByteArray()));
        StarterTest.check(decoded.id() == 42 && decoded.operation().equals(Operations.key(method))
                && decoded.arguments()[1].equals(new Vector<>(List.of("512", "513")))
                && decoded.arguments()[2].equals("Montréal\nToronto"), "general wrapper preserves typed bundle arguments");
        StarterTest.check(!Operations.key(Operations.method("newCustomer")).equals(
                Operations.key(Operations.method("newCustomer", int.class))), "overloads have distinct signatures");
        output.reset();
        Protocol.write(output, new Protocol.Response(42, new Server.Common.ReservationReceipt("flight-512", 2, 100), null));
        Protocol.write(output, new Protocol.Response(43, "Bill\nTotal cost: $200\n", null));
        InputStream fragmented = new ByteArrayInputStream(output.toByteArray()) {
            public synchronized int read(byte[] bytes, int offset, int length) { return super.read(bytes, offset, Math.min(length, 1)); }
        };
        var receipt = (Server.Common.ReservationReceipt)((Protocol.Response)Protocol.read(fragmented)).value();
        StarterTest.check(receipt.getQuantity() == 2 && receipt.getPrice() == 100
                && ((Protocol.Response)Protocol.read(fragmented)).id() == 43, "split and joined frames preserve replies");
        try {
            Protocol.read(new ByteArrayInputStream(new byte[]{127, -1, -1, -1}));
            throw new AssertionError("oversized frame accepted");
        } catch (IOException expected) { }
        for (var operation : Server.Interface.IInventoryManager.class.getMethods()) {
            Object[] values = java.util.Arrays.stream(operation.getParameterTypes()).map(type ->
                    type == int.class ? 7 : type == boolean.class ? true : type == Vector.class ? new Vector<>(List.of("512")) : "Montreal").toArray();
            output.reset(); Protocol.write(output, new Protocol.Request(1, Operations.key(operation), values));
            var copy = (Protocol.Request)Protocol.read(new ByteArrayInputStream(output.toByteArray()));
            StarterTest.check(Operations.resolve(copy.operation(), copy.arguments(), true).equals(operation), "every signature uses same wrapper");
        }
        System.out.println("PASS TcpProtocolTest");
    }
}
