package com.elvis.cliente.models;

import java.util.List;
import java.util.Map;

public class FCMBody {
    private List<String> registration_ids;
    private String priority;
    private String ttl;
    Map<String, String> data;

    public FCMBody(List<String> registration_ids, String priority, String ttl, Map<String, String> data) {
        this.registration_ids = registration_ids;
        this.priority = priority;
        this.ttl = ttl;
        this.data = data;
    }

    public List<String> getRegistration_ids() {
        return registration_ids;
    }

    public void setRegistration_ids(List<String> registration_ids) {
        this.registration_ids = registration_ids;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}