/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.romankh3.image.comparison.model.Rectangle;
import com.salesforce.cte.common.TestEventType;

/**
 * @author Yibing TAo
 * Base class for test case signal
 */
public class TestSignalBase implements TestAdvisorTestSignal{
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private TestEventType signalName;
    private String signalValue;
    private Instant signalTime;
    private Level signalLevel = Level.INFO;
    private String seleniumCmd;
    private String seleniumParam;
    private String seleniumLocator;
    private int screenshotRecorderNumber;
    private String screenshotPath;
    private List<Rectangle> excludedAreas = new ArrayList<>();

    public TestSignalBase(TestEventType name, String value, Instant time){
        signalName = name ;
        signalValue = value == null ? "" : value;
        signalTime = time;
    }

    public TestSignalBase(TestEventType name, String value, Instant time, 
        String level, String cmd, String param, String locator, int num, String path){
        this(name,value,time);
        try{
            signalLevel = Level.parse(level);
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Invalid level {0}", level);
        }
        seleniumCmd = cmd == null ? "" : cmd;
        seleniumParam = param == null ? "" : param;
        seleniumLocator = locator == null ? "" : locator;
        screenshotRecorderNumber = num;
        screenshotPath = path == null ? "" : path;
    }

    @Override
    public TestEventType getTestSignalName() {
        return signalName;
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
    public Level getTestSignalLevel() {
        return signalLevel;
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

