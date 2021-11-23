package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;

import com.github.romankh3.image.comparison.model.Rectangle;

/**
 * @author Yibing Tao
 * ITestSignal interface define how to get signal for a single test case
 */
public interface TestAdvisorTestSignal {
    public String getTestSignalName();
    public String getTestSignalValue();
    public Instant getTestSignalTime();
    public String getTestSignalLevel();
    public String getTestSignalSeleniumCmd();
    public String getTestSignalSeleniumParam();
    public String getTestSignalSeleniumLocator();
    public int getTestSignalScreenshotRecorderNumber();
    public String getTestSignalScreenshotPath();
    public List<Rectangle> getExcludedAreas();
    public void setExcludedAreas(List<Rectangle> excludedAreas);
}
