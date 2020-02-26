package com.hazelcast.cdc.pojo;

import java.io.Serializable;

public class ProductKey implements Serializable {

    public Long sku = 0L;
    public String skuHash = null;

    public ProductKey() {}

    @Override
    public String toString() {
        return "ProductKey{" +
                "sku=" + sku +
                ", skuHash=" + skuHash +
                '}';
    }
}