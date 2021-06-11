package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;

/**
 * @auther Yibing TAo
 * TestNG test case signal
 */
public class TestNGSignal implements ITestSignal{

    private String signalName;
    private String signalValue;
    private ZonedDateTime signalTime;

    public TestNGSignal(String name, String value, ZonedDateTime time){
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
