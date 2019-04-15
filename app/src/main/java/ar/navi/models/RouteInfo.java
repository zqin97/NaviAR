package ar.navi.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class RouteInfo {

    private LatLng startLocation;
    private LatLng destination;
    private ArrayList<LatLng> routePoints;

    public RouteInfo(LatLng start, LatLng end, List<LatLng> points){
        startLocation = start;
        destination = end;
        routePoints = new ArrayList<>(points);
    }

    public LatLng getStartLocation(){
        return startLocation;
    }

    public LatLng getDestination(){
        return destination;
    }

    public ArrayList<LatLng> getRoutePoints(){
        return routePoints;
    }
}
