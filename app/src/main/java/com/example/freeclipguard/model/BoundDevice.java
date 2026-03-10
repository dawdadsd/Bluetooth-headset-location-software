package com.example.freeclipguard.model;

public final class BoundDevice {

    private final String name;
    private final String address;

    public BoundDevice(String name, String address) {
        this.name = name == null ? "未命名设备" : name;
        this.address = address == null ? "" : address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public boolean isConfigured() {
        return !address.isBlank();
    }

    public boolean matchesAddress(String otherAddress) {
        return otherAddress != null && address.equalsIgnoreCase(otherAddress);
    }
}
