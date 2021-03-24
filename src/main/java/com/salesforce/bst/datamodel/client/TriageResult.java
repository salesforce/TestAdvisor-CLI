package com.salesforce.bst.datamodel.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao This class defines test case triage result from BST
 *         backend
 */
public class TriageResult {
    @JsonProperty
    private String  regressionType;
    @JsonProperty
    private String  regressionReason;
    @JsonProperty
    private int     confidence; // 0 - 100, 0 == impossible, 100 == sure
    @JsonProperty
    private String  recommendation;

    public TriageResult(String type, String reason, int confidence, String recommend){
        this.regressionType = type;
        this.regressionReason = reason;
        this.confidence = confidence;
        this.recommendation = recommend;
    }

    public String getRegressionType(){
        return this.regressionType;
    }

    public String getRegressionReason(){
        return this.regressionReason;
    }

    public int getConfidence(){
        return this.confidence;
    }

    public String getRecommendation(){
        return this.recommendation;
    }
}
