package com.salesforce.bst.datamodel.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao This class defines customer information during BST signup
 */
public class Customer {
    @JsonProperty
    private String customerId; // Regex "[a-zA-Z0-9]{1,80}"
    @JsonProperty
    private String sandboxInstanceName; // Regex "[a-zA-Z0-9]{1,15}"
    @JsonProperty
    private String sandboxOrgId;
    @JsonProperty
    private String sandboxOrgName; // Regex "[a-zA-Z0-9]{1,80}"

    public Customer() {}

    public Customer(String id, String instanceName, String orgId, String orgName){
        this.customerId = id;
        this.sandboxInstanceName = instanceName;
        this.sandboxOrgId = orgId;
        this.sandboxOrgName = orgName;
    }

    public String getCustomerId(){
        return this.customerId;
    }

    public String getSandboxInstanceName(){
        return this.sandboxInstanceName;
    }

    public String getSandboxOrgId(){
        return this.sandboxOrgId;
    }

    public String getSandboxOrgName(){
        return this.sandboxOrgName;
    }
}
