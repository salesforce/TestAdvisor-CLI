/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

 package com.salesforce.cte.datamodel.client;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestExecution {
    
    //Datetime format https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT
    //Salesforce SQQL date format 
    //https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_dateformats.htm
    //example 2011-12-03T10:15:30Z
    @JsonProperty
    public Instant    startTime; 
    @JsonProperty
    public Instant    endTime;    
    @JsonProperty
    public TestStatus status;
    @JsonProperty
    public boolean    isConfiguration;
    @JsonProperty
    public String     traceId = "";
    @JsonProperty
    public String  testCaseName = ""; // Regex "[a-zA-Z0-9\-_]{1,80}"
    @JsonProperty
    public String   baselineBuildId = ""; // Regex "[a-zA-Z0-9]{1,20}" 
    @JsonProperty
    public Instant   baselineBuildIdStartTime; 
    @JsonProperty
    public String   baselineSalesforceBuildId = ""; // Regex "[a-zA-Z0-9]{1,20}" 
    @JsonProperty
    public int  similarity; //0-100
    @JsonProperty
    public List<TestSignal> testSignals = new ArrayList<>();
}
