package Server.Common;

import java.io.Serializable;

/** Stage 2: immutable booked quantity/price passed across either transport. */
public final class ReservationReceipt implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String key;
    private final int quantity;
    private final int price;

    public ReservationReceipt(String key, int quantity, int price) {
        this.key = key;
        this.quantity = quantity;
        this.price = price;
    }

    public String getKey() { return key; }
    public int getQuantity() { return quantity; }
    public int getPrice() { return price; }
}
