package com.elvis.cliente.models;

public class Driver {

    String id;
    String name;
    String email;
    String vehicleBrand;
    String vehiclePlate;
    String image;

    public Driver() {

    }

    public Driver(String id, String name, String email, String vehicleBrand, String vehiclePlate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.vehicleBrand = vehicleBrand;
        this.vehiclePlate = vehiclePlate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }
}
