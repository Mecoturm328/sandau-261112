package com.mori.dashboard.entity;

/**
 * 商品表 products
 */
public class Product {

    private Long id;
    private String name;
    private String category;
    private Double price;
    private Integer sortOrder;

    public Product() {
    }

    public Product(
            Long id,
            String name,
            String category,
            Double price,
            Integer sortOrder
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.sortOrder = sortOrder;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
