package com.salesforce.bst.datamodel.client;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 * This class defines the BST client registry information on which
 * all test results are stored
 */
public class Registry {
    @JsonProperty
    private UUID registryGuid;
    @JsonProperty
    private String registryName; // Regex "[a-zA-Z0-9]{1,80}"
    @JsonProperty
    private String bstClientVersion;

    public Registry() {}
    public Registry(UUID id, String name, String version){
        this.registryGuid = id;
        this.registryName = name;
        this.bstClientVersion = version;
    }

    public UUID getRegistryGuid(){
        return this.registryGuid;
    }

    public String getRegistryName(){
        return this.registryName;
    }

    public String getBstClientVersion(){
        return this.bstClientVersion;
    }
}
