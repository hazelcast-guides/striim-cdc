package com.hazelcast.cdc.hazelcast;

import com.hazelcast.cdc.pojo.ProductInv;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class Client {

    public static void main(String[] args) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("{HZ_IP_ADDRESS}");

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
        IMap<Long, ProductInv> productInv = client.getMap("ProductInv");

        System.out.println(productInv.size());
    }
}
