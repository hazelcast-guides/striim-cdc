package com.hazelcast.cdc.oracle;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "compound_product_inv")
@IdClass(ProductKey.class)
public class ProductInv implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long sku = 0L;

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    public String skuHash = null;

    public double stock = 0;
    public String name = null;
    public Date lastUpdated = null;

    public ProductInv() {
    }

    public ProductInv(double stock, String name, Date lastUpdated) {
        this.stock = stock;
        this.name = name;
        this.lastUpdated = lastUpdated;
    }

    public Long getSku() {
        return sku;
    }

    public void setSku(Long sku) {
        this.sku = sku;
    }

    public String getSkuHash() {
        return skuHash;
    }

    public void setSkuHash(String skuHash) {
        this.skuHash = skuHash;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductInv that = (ProductInv) o;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {

        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "ProductInv{" +
                "sku=" + sku +
                ", skuHash=" + skuHash +
                ", stock=" + stock +
                ", name='" + name + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
