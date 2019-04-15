package ar.navi.models;

import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class PlaceDetails {

    private String name;
    private String address;
    private String phoneNumber;
    private String id;
    private String attributions;
    private float rating;
    private LatLng latLng;
    private Uri website;

    public PlaceDetails(String name, String address, String phoneNumber, String id, String attributions,
                        float rating, LatLng latLng, Uri website) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.id = id;
        this.attributions = attributions;
        this.rating = rating;
        this.latLng = latLng;
        this.website = website;
    }

    public PlaceDetails() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttributions() {
        return attributions;
    }

    public void setAttributions(String attributions) {
        this.attributions = attributions;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public Uri getWebsite() {
        return website;
    }

    public void setWebsite(Uri website) {
        this.website = website;
    }
}
