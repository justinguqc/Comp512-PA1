package Tests;

import Server.Common.Middleware;
import Server.Common.ResourceManager;
import Server.Interface.IResourceManager;

/** Stage 3: public behavior across three independent inventory services. */
public final class MiddlewareTest {
    public static void main(String[] args) throws Exception {
        ResourceManager flights = new ResourceManager("Flights");
        ResourceManager cars = new ResourceManager("Cars");
        ResourceManager rooms = new ResourceManager("Rooms");
        IResourceManager middleware = new Middleware("Middleware", flights, cars, rooms);
        middleware.addFlight(512, 2, 100);
        middleware.addCars("Montreal", 2, 30);
        middleware.addRooms("Montreal", 2, 80);
        StarterTest.check(flights.queryFlight(512) == 2 && cars.queryFlight(512) == 0,
                "flight inventory is routed only to Flights");
        StarterTest.check(cars.queryCars("Montreal") == 2 && rooms.queryCars("Montreal") == 0,
                "car inventory is routed only to Cars");
        StarterTest.check(middleware.newCustomer(7), "create customer at middleware");
        StarterTest.check(!middleware.newCustomer(7), "duplicate customer rejected");
        StarterTest.check(middleware.reserveFlight(7, 512), "reserve flight through middleware");
        StarterTest.check(middleware.reserveCar(7, "Montreal"), "reserve car through middleware");
        StarterTest.check(middleware.reserveRoom(7, "Montreal"), "reserve room through middleware");
        StarterTest.check(middleware.queryCustomerInfo(7).contains("Total cost: $210"), "combined bill");
        StarterTest.check(flights.queryCustomerInfo(7).isEmpty() && cars.queryCustomerInfo(7).isEmpty()
                && rooms.queryCustomerInfo(7).isEmpty(), "customer records stay exclusively at middleware");
        StarterTest.check(middleware.deleteCustomer(7), "delete customer across all managers");
        StarterTest.check(middleware.queryCustomerInfo(7).isEmpty(), "deleted customer no longer exists");
        StarterTest.check(middleware.queryFlight(512) == 2 && middleware.queryCars("Montreal") == 2
                && middleware.queryRooms("Montreal") == 2, "deletion returns all reservations");
        StarterTest.check(!middleware.deleteCustomer(7), "deleting a missing customer fails");
        middleware.newCustomer(1);
        int generated = middleware.newCustomer();
        StarterTest.check(generated != 1 && middleware.queryCustomerInfo(generated).contains("Total cost: $0"),
                "generated IDs do not overwrite explicit customer IDs");
        System.out.println("PASS MiddlewareTest");
    }
}
