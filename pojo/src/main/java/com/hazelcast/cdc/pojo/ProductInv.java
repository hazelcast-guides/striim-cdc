package com.hazelcast.cdc.pojo;

import java.io.Serializable;
import java.util.Date;

public class ProductInv implements Serializable {

    public Long sku = 0L;
    public String skuHash = null;

    public double stock = 0;
    public String name = null;
    public Date lastUpdated = null;

    public ProductInv() {}

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
