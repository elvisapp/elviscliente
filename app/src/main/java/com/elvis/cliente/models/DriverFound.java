package com.elvis.cliente.models;

public class DriverFound {

    private String idDriver;
    private String idClientBooking;

    public DriverFound() {
    }

    public DriverFound(String idDriver, String idClientBooking) {
        this.idDriver = idDriver;
        this.idClientBooking = idClientBooking;
    }


    public String getIdDriver() {
        return idDriver;
    }

    public void setIdDriver(String idDriver) {
        this.idDriver = idDriver;
    }

    public String getIdClientBooking() {
        return idClientBooking;
    }

    public void setIdClientBooking(String idClientBooking) {
        this.idClientBooking = idClientBooking;
    }
}
