package hu.krisz768.bettertuke.Database;

import android.content.Context;

import java.io.Serializable;

public class BusLine implements Serializable {
    private final int LineId;
    private final int DepartureHour;
    private final int DepartureMinute;
    private final LineInfoTravelTime[] Stops;
    private final LineInfoRoute[] Route;
    private final LineInfoRouteInfo RouteInfo;
    private String Date;

    public LineInfoRouteInfo getRouteInfo() {
        return RouteInfo;
    }

    public int getLineId() {
        return LineId;
    }

    public int getDepartureHour() {
        return DepartureHour;
    }

    public int getDepartureMinute() {
        return DepartureMinute;
    }

    public LineInfoTravelTime[] getStops() {
        return Stops;
    }

    public LineInfoRoute[] getRoute() {
        return Route;
    }

    public BusLine(int lineId, int departureHour, int departureMinute, LineInfoTravelTime[] stops, LineInfoRoute[] route, LineInfoRouteInfo routeInfo) {
        LineId = lineId;
        DepartureHour = departureHour;
        DepartureMinute = departureMinute;
        Stops = stops;
        Route = route;
        RouteInfo = routeInfo;
    }

    public static BusLine BusLinesByLineId(int Id, Context ctx) {
        DatabaseManager Dm = new DatabaseManager(ctx);

        return Dm.GetBusLineById(Id);
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }
}
