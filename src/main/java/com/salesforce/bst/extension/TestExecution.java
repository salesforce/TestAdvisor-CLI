package com.salesforce.bst.extension;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestExecution {
    
    //Datetime format https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT
    //Salesforce SQQL date format 
    //https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_dateformats.htm
    //example 2011-12-03T10:15:30Z
    @JsonProperty
    public String    startTime; 
    @JsonProperty
    public String    endTime;    
    @JsonProperty
    public TestStatus status;
    @JsonProperty
    public String  testCaseName; // Regex "[a-zA-Z0-9\-_]{1,80}"
    @JsonProperty
    public List<TestSignal> testSignals;
}
