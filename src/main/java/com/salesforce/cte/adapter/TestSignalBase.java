package com.salesforce.cte.adapter;

import java.time.ZonedDateTime;

/**
 * @author Yibing TAo
 * Base class for test case signal
 */
public class TestSignalBase implements TestAdvisorTestSignal{

    private String signalName;
    private String signalValue;
    private ZonedDateTime signalTime;

    public TestSignalBase(String name, String value, ZonedDateTime time){
        signalName = name;
        signalValue = value;
        signalTime = time;
    }

    @Override
    public String getTestSignalName() {
        return signalName;
    }

    @Override
    public String getTestSignalValue() {
        return signalValue;
    }

    @Override
    public ZonedDateTime getTestSignalTime() {
        return signalTime;
    }
    
}
