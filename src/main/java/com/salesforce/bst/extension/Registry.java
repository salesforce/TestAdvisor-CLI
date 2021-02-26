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
    private TimeZone timezone;
    
    public Registry(UUID id){
        this.registryGuid = id;
    }

    public Registry(UUID id, String name, TimeZone timezone){
        this(id);
        this.registryName = name;
        this.timezone = timezone;
    }

    public UUID getRegistryGuid(){
        return this.registryGuid;
    }

    public String getRegistryName(){
        return this.registryName;
    }

    public TimeZone getTimeZone(){
        return this.timezone;
    }
}
