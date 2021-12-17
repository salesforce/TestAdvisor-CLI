/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

import com.github.romankh3.image.comparison.model.Rectangle;

/**
 * @author Yibing Tao
 * ITestSignal interface define how to get signal for a single test case
 */
public interface TestAdvisorTestSignal {
    public String getTestSignalName();
    public String getTestSignalValue();
    public Instant getTestSignalTime();
    public Level getTestSignalLevel();
    public String getTestSignalSeleniumCmd();
    public String getTestSignalSeleniumParam();
    public String getTestSignalSeleniumLocator();
    public int getTestSignalScreenshotRecorderNumber();
    public String getTestSignalScreenshotPath();
    public List<Rectangle> getExcludedAreas();
    public void setExcludedAreas(List<Rectangle> excludedAreas);
}
