package io.fabric8.quickstarts.springbootwebmvcip;

public class IPAddress {

    private final long id;

    private final String ipAddress;

    public IPAddress(long id, String ipAddress) {
        this.id = id;
        this.ipAddress = ipAddress;
    }

    public long getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
