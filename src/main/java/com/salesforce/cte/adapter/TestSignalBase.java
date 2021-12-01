/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;

import com.github.romankh3.image.comparison.model.Rectangle;

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
    private List<Rectangle> excludedAreas;

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
        return signalName == null ? "" : signalName;
    }

    @Override
    public String getTestSignalValue() {
        return signalValue == null ? "" : signalValue;
    }

    @Override
    public Instant getTestSignalTime() {
        return signalTime;
    }

    @Override
    public String getTestSignalLevel() {
        return signalLevel == null ? "" : signalLevel;
    }

    @Override
    public String getTestSignalSeleniumCmd() {
        return seleniumCmd == null ? "" : seleniumCmd;
    }

    @Override
    public String getTestSignalSeleniumParam() {
        return seleniumParam == null ? "" : seleniumParam;
    }

    @Override
    public String getTestSignalSeleniumLocator() {
        return seleniumLocator == null ? "" : seleniumLocator;
    }

    @Override
    public int getTestSignalScreenshotRecorderNumber() {
        return screenshotRecorderNumber;
    }

    @Override
    public String getTestSignalScreenshotPath() {
        return screenshotPath == null ? "" : screenshotPath;
    }
    
    @Override
    public List<Rectangle> getExcludedAreas() {
        return excludedAreas;
    }

    @Override
    public void setExcludedAreas(List<Rectangle> excludedAreas){
        this.excludedAreas = excludedAreas;
    }
}

