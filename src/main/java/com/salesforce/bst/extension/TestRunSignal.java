package com.salesforce.bst.extension;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 * This class defines all properties as signals to indicate potential regressions
 * for a test run.
 */
public class TestRunSignal {
    
    //Datetime format https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT
    //Salesforce SQQL date format 
    //https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_dateformats.htm
    //example 2011-12-03T10:15:30Z
    @JsonProperty
    public String   buildStartTime; 
    @JsonProperty
    public String   buildEndTime;
    @JsonProperty
    public String   clientBuildId; // Regex "[a-zA-Z0-9]{1,20}" 
    @JsonProperty
    public UUID     clientRegistryGuid;
    @JsonProperty
    public String   clientCliVersion; // Regex "[a-zA-Z0-9\.]{1,20}"
    @JsonProperty
    public String   clientLibraryVersion; // Regex "[a-zA-Z0-9\.]{1,20}"
    @JsonProperty
    public String  sandboxInstance; // Regex "[a-zA-Z0-9]{1,20}" 
    @JsonProperty
    public String  sandboxOrgId; // Regex "[a-zA-F0-9]{15}" https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/field_types.htm#i1435616
    @JsonProperty
    public String  sandboxOrgName; // Regex "[a-zA-Z0-9]{1,80}"
    @JsonProperty   
    public String  testSuiteName; // Regex "[a-zA-Z0-9\-_]{1,80}"
    @JsonProperty
    public List<TestExecution> testExecutions;
    
}
