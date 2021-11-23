package com.salesforce.cte.datamodel.client;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.romankh3.image.comparison.model.Rectangle;

/**
 * @author Yibing Tao
 * This class defines all properties as signals to indicate potential regressions
 * for a test case.
 * Regression signals could be collected from test run or Salesforce internal system.
 */
public class TestSignal {
    
    @JsonProperty
    public String   signalName;    
    @JsonProperty
    public String   signalValue;    // Regex "[a-zA-Z0-9\-\._]{1,1000}"
    @JsonProperty
    public Instant   signalTime;
    @JsonProperty
    public Instant   previousSignalTime;
    @JsonProperty
    public String   seleniumCmd;
    @JsonProperty
    public String   locatorHash;
    @JsonProperty
    public String   locator;
    @JsonProperty
    public int   screenshotRecorderNumber;
    @JsonProperty
    public int   baselinScreenshotRecorderNumber;
    @JsonProperty
    public int screenshotDiffRatio;
    @JsonProperty
    public List<Rectangle> screenshotDiffAreas;
    @JsonProperty
    public String   errorMessage;
}
