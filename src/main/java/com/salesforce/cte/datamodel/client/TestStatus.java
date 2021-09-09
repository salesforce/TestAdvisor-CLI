package com.salesforce.cte.datamodel.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 */
public enum TestStatus {
    @JsonProperty("Failed")
    FAIL,
    @JsonProperty("Passed")
    PASS,
    @JsonProperty("Skipped")
    SKIP
}
