package com.salesforce.cte.adapter;

import java.time.Instant;

/**
 * @author Yibing TAo
 * Base class for test case signal
 */
public class TestSignalBase implements TestAdvisorTestSignal{

    private String signalName;
    private String signalValue;
    private Instant signalTime;
    private String signalLevel;
    private String seleniumCmd;
    private String seleniumParam;
    private String seleniumLocator;
    private int screenshotRecorderNumber;
    private String screenshotPath;

    public TestSignalBase(String name, String value, Instant time){
        signalName = name == null ? "" : name;
        signalValue = value == null ? "" : value;
        signalTime = time;
    }

    public TestSignalBase(String name, String value, Instant time, 
        String level, String cmd, String param, String locator, int num, String path){
        this(name,value,time);
        signalLevel = level == null ? "" : level;
        seleniumCmd = cmd == null ? "" : cmd;
        seleniumParam = param == null ? "" : param;
        seleniumLocator = locator == null ? "" : locator;
        screenshotRecorderNumber = num;
        screenshotPath = path == null ? "" : path;
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
    public Instant getTestSignalTime() {
        return signalTime;
    }

    @Override
    public String getTestSignalLevel() {
        return signalLevel;
    }

    @Override
    public String getTestSignalSeleniumCmd() {
        return seleniumCmd;
    }

    @Override
    public String getTestSignalSeleniumParam() {
        return seleniumParam;
    }

    @Override
    public String getTestSignalSeleniumLocator() {
        return seleniumLocator;
    }

    @Override
    public int getTestSignalScreenshotRecorderNumber() {
        return screenshotRecorderNumber;
    }

    @Override
    public String getTestSignalScreenshotPath() {
        return screenshotPath;
    }
    
}
