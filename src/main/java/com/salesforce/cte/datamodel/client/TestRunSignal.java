/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.datamodel.client;

import java.time.Instant;
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
    
    @JsonIgnore
    public String   testRunId; //test run id set by registry, format TestRun-yyyyMMdd-HHmmss
    //Datetime format https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_INSTANT
    //Salesforce SQQL date format 
    //https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_dateformats.htm
    //example 2011-12-03T10:15:30Z
    @JsonProperty
    public Instant   buildStartTime; 
    @JsonProperty
    public Instant   buildEndTime;
    @JsonProperty
    public String   clientBuildId; // Regex "[a-zA-Z0-9]{1,20}" 
    @JsonProperty
    public UUID     clientRegistryGuid; //version 4 UUID  https://datatracker.ietf.org/doc/html/rfc4122#section-4.4
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
