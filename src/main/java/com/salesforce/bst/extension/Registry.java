package com.salesforce.bst.extension;

import java.util.TimeZone;
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
    private TimeZone timeZone;
    
    public Registry(UUID id){
        this.registryGuid = id;
    }

    public Registry(UUID id, String name, TimeZone timeZone){
        this(id);
        this.registryName = name;
        this.timeZone = timeZone;
    }

    public UUID getRegistryGuid(){
        return this.registryGuid;
    }

    public String getRegistryName(){
        return this.registryName;
    }

    public TimeZone timeZone(){
        return this.timeZone;
    }
}
