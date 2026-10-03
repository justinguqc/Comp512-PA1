package Server.TCP;

import Server.Common.ReservationReceipt;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

/** Stage 5: general length-prefixed, typed wrapping for every client/backend operation.
 * No RMI calls or Java object deserialization occur on TCP links. */
public final class Protocol {
    public static final int MAX_FRAME = 1_048_576;
    public record Request(long id, String operation, Object[] arguments) { }
    public record Response(long id, Object value, String error) { }
    private Protocol() { }

    public static void write(OutputStream stream, Object message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeInt(1); // Protocol version.
        if (message instanceof Request request) {
            data.writeByte(1); data.writeLong(request.id()); string(data, request.operation());
            data.writeInt(request.arguments().length);
            for (Object argument : request.arguments()) value(data, argument);
        } else if (message instanceof Response response) {
            data.writeByte(2); data.writeLong(response.id());
            value(data, response.error()); value(data, response.value());
        } else throw new IOException("Unsupported message");
        if (bytes.size() > MAX_FRAME) throw new IOException("Frame too large");
        DataOutputStream output = new DataOutputStream(stream);
        output.writeInt(bytes.size()); bytes.writeTo(output); output.flush();
    }

    public static Object read(InputStream stream) throws IOException {
        DataInputStream input = new DataInputStream(stream);
        int size = bounded(input.readInt(), MAX_FRAME);
        byte[] bytes = new byte[size]; input.readFully(bytes);
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        if (data.readInt() != 1) throw new IOException("Unsupported protocol version");
        int kind = data.readUnsignedByte(); long id = data.readLong(); Object message;
        if (kind == 1) {
            String operation = string(data);
            Object[] args = new Object[bounded(data.readInt(), 32)];
            for (int i = 0; i < args.length; i++) args[i] = value(data);
            message = new Request(id, operation, args);
        } else if (kind == 2) {
            Object error = value(data);
            if (error != null && !(error instanceof String)) throw new IOException("Invalid error");
            message = new Response(id, value(data), (String)error);
        } else throw new IOException("Invalid message kind");
        if (data.available() != 0) throw new IOException("Trailing frame data");
        return message;
    }

    private static int bounded(int number, int limit) throws IOException {
        if (number < 0 || number > limit) throw new IOException("Invalid field length");
        return number;
    }
    private static void string(DataOutputStream data, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        bounded(bytes.length, MAX_FRAME); data.writeInt(bytes.length); data.write(bytes);
    }
    private static String string(DataInputStream data) throws IOException {
        byte[] bytes = new byte[bounded(data.readInt(), MAX_FRAME)]; data.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    private static void value(DataOutputStream data, Object value) throws IOException {
        if (value == null) data.writeByte(0);
        else if (value instanceof Integer number) { data.writeByte(1); data.writeInt(number); }
        else if (value instanceof Boolean flag) { data.writeByte(2); data.writeBoolean(flag); }
        else if (value instanceof String text) { data.writeByte(3); string(data, text); }
        else if (value instanceof Vector<?> items) {
            data.writeByte(4); bounded(items.size(), 10000); data.writeInt(items.size());
            for (Object item : items) {
                if (!(item instanceof String)) throw new IOException("Expected string vector");
                string(data, (String)item);
            }
        } else if (value instanceof ReservationReceipt receipt) {
            data.writeByte(5); string(data, receipt.getKey()); data.writeInt(receipt.getQuantity()); data.writeInt(receipt.getPrice());
        } else throw new IOException("Unsupported value type");
    }
    private static Object value(DataInputStream data) throws IOException {
        switch (data.readUnsignedByte()) {
            case 0: return null;
            case 1: return data.readInt();
            case 2: return data.readBoolean();
            case 3: return string(data);
            case 4:
                Vector<String> items = new Vector<>();
                int count = bounded(data.readInt(), 10000);
                for (int i = 0; i < count; i++) items.add(string(data));
                return items;
            case 5: return new ReservationReceipt(string(data), data.readInt(), data.readInt());
            default: throw new IOException("Invalid value tag");
        }
    }
}
