package com.hazelcast.cdc.oracle;

import java.io.Serializable;

public class ProductKey implements Serializable {

    public Long sku = 0L;
    public String skuHash = null;

    public ProductKey() {
    }

    public ProductKey(Long sku, String skuHash) {
        this.sku = sku;
        this.skuHash = skuHash;
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

    @Override
    public String toString() {
        return "ProductKey{" +
                "sku=" + sku +
                ", skuHash=" + skuHash +
                '}';
    }
}