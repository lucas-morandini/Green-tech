package com.example.frontend.utils;

import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.util.GeoPoint;

import java.util.Date;

public class Scan implements Parcelable {
    private GeoPoint scan_point;
    private Date scan_date;

    public Scan(GeoPoint scan_point, Date scan_date) {
        this.scan_point = scan_point;
        this.scan_date = scan_date;
    }

    protected Scan(Parcel in) {
        double latitude = in.readDouble();
        double longitude = in.readDouble();
        this.scan_point = new GeoPoint(latitude, longitude);
        long time = in.readLong();
        this.scan_date = new Date(time);
    }

    public static final Creator<Scan> CREATOR = new Creator<Scan>() {
        @Override
        public Scan createFromParcel(Parcel in) {
            return new Scan(in);
        }

        @Override
        public Scan[] newArray(int size) {
            return new Scan[size];
        }
    };

    public GeoPoint getScan_point() {
        return scan_point;
    }

    public Date getScan_date() {
        return scan_date;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(scan_point.getLatitude());
        dest.writeDouble(scan_point.getLongitude());
        dest.writeLong(scan_date.getTime());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
