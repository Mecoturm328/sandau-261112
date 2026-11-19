package com.mori.dashboard.entity;

/**
 * 门店表 stores
 */
public class Store {

    private Long id;
    private String name;
    private String shortName;
    private String address;
    private String accent;

    public Store() {
    }

    public Store(
            Long id,
            String name,
            String shortName,
            String address,
            String accent
    ) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.address = address;
        this.accent = accent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }
}
