package ar.navi.models;

import com.google.android.gms.maps.model.Polyline;
import com.google.maps.model.DirectionsLeg;

import java.io.Serializable;

public class PolylineRoute implements Serializable {

    private Polyline polyLine;
    private DirectionsLeg leg;

    public PolylineRoute(Polyline polyLine, DirectionsLeg leg){
        this.polyLine = polyLine;
        this.leg = leg;
    }

    public Polyline getPolyLine(){
        return polyLine;
    }

    public DirectionsLeg getLeg(){
        return leg;
    }

    public String getLegInfo(){
        return "Duration: " + leg.duration.toString() +
                "\n" + "Distance: " + leg.distance.toString() +"\n";
    }

    public void setLeg(DirectionsLeg leg){
        this.leg = leg;
    }

    @Override
    public String toString() {
        return "PolylineData{" +
                "polyline=" + polyLine +
                ", leg=" + leg +
                '}';
    }
}
