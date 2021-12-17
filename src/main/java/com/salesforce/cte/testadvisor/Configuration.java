/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yibing Tao
 * Provides global configruaiton settings for TestAdvisor CLI
 */
public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private static final String SCREENHSOT_COMPARISON_PROPERTY = "testadvisor.screenshotcomparison";
    private static final String SIGNAL_LEVEL_PROPERTY = "testadvisor.signallevel";
    private static final String EXPORT_SCREENSHOT_DIFF_AREA_PROPERTY = "testadvisor.exportscreenshotdiffarea";
    private static final String SCREENSHOT_MIN_DIFF_AREA_SIZE = "testadvisor.screenshotmindiffareasize";

    //private constructor to prevent instance
    private Configuration() {}

    public static boolean getIsScreenshotComparisonEnabled(){
        return Boolean.parseBoolean(System.getProperty(SCREENHSOT_COMPARISON_PROPERTY, "false"));
    }

    public static Level getSignalLevel(){
        //Supported Level, OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
        return Level.parse(System.getProperty(SIGNAL_LEVEL_PROPERTY, "WARNING"));
    }

    public static boolean getExportScreenshotDiffArea(){
        return Boolean.parseBoolean(System.getProperty(EXPORT_SCREENSHOT_DIFF_AREA_PROPERTY, "false"));
    }

    public static int getScreenshotMinDiffAreaSize(){
        try{
            return Integer.parseInt(System.getProperty(SCREENSHOT_MIN_DIFF_AREA_SIZE, "20"));
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Invalid system property testadvisor.screenshotmindiffareasize");
            return 20;
        }   
    }
}
