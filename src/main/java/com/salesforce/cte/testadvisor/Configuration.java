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
    private static final String FALSE = "false";

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private static final String SELENIUM_URL_PROPERTY = "testadvisor.selenium.url";
    private static final String SELENIUM_EXCEPTION_PROPERTY = "testadvisor.selenium.exception";
    private static final String SELENIUM_ERROR_MESSAGE_PROPERTY = "testadvisor.selenium.errormessage";
    private static final String SELENIUM_SCREENSHOT_PROPERTY = "testadvisor.selenium.screenshot";
    private static final String SIGNAL_LEVEL_PROPERTY = "testadvisor.signallevel";
    private static final String EXPORT_SCREENSHOT_DIFF_AREA_PROPERTY = "testadvisor.exportscreenshotdiffarea";
    private static final String EXPORT_SCREENSHOT_DIFF_IMAGE_PROPERTY = "testadvisor.exportscreenshotdiffimage";
    private static final String SCREENSHOT_MIN_DIFF_AREA_SIZE_PROPERTY = "testadvisor.screenshotmindiffareasize";
    private static final String SCREENSHOT_MIN_DIFF_RATIO_PROPERTY = "testadvisor.screenshotmindiffratio";
    private static final String UPLOAD_ALL_CONF_TEST_PROPERTY = "testadvisor.uploadallconfigurationtest";

    //private constructor to prevent instance
    private Configuration() {}

    public static boolean getIsSeleniumUrlEnabled(){
        return Boolean.parseBoolean(System.getProperty(SELENIUM_URL_PROPERTY, FALSE));
    }

    public static boolean getIsSeleniumExceptionEnabled(){
        return Boolean.parseBoolean(System.getProperty(SELENIUM_EXCEPTION_PROPERTY, FALSE));
    }

    public static boolean getIsSeleniumErrorMessageEnabled(){
        return Boolean.parseBoolean(System.getProperty(SELENIUM_ERROR_MESSAGE_PROPERTY, FALSE));
    }

    public static boolean getIsSeleniumScreenshotEnabled(){
        return Boolean.parseBoolean(System.getProperty(SELENIUM_SCREENSHOT_PROPERTY, FALSE));
    }

    public static Level getSignalLevel(){
        //Supported Level, OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
        return Level.parse(System.getProperty(SIGNAL_LEVEL_PROPERTY, "WARNING"));
    }

    public static boolean getExportScreenshotDiffArea(){
        return Boolean.parseBoolean(System.getProperty(EXPORT_SCREENSHOT_DIFF_AREA_PROPERTY, FALSE));
    }

    public static boolean getExportScreenshotDiffImage(){
        return Boolean.parseBoolean(System.getProperty(EXPORT_SCREENSHOT_DIFF_IMAGE_PROPERTY, FALSE));
    }

    public static int getScreenshotMinDiffAreaSize(){
        try{
            return Integer.parseInt(System.getProperty(SCREENSHOT_MIN_DIFF_AREA_SIZE_PROPERTY, "20"));
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Invalid system property testadvisor.screenshotmindiffareasize {0}", 
                System.getProperty(SCREENSHOT_MIN_DIFF_AREA_SIZE_PROPERTY, "20"));
            return 20;
        }   
    }

    public static int getScreenshotMinDiffRatio(){
        try{
            return Integer.parseInt(System.getProperty(SCREENSHOT_MIN_DIFF_RATIO_PROPERTY, "1"));
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Invalid system property testadvisor.screenshotmindiffratio {0}",
                System.getProperty(SCREENSHOT_MIN_DIFF_RATIO_PROPERTY));
            return 1;
        }   
    }

    /**
     * If upload all configuration test signals
     * @return
     * false, default value, only upload failed configuration test signals
     * true, upload all configuration test signals
     */
    public static boolean getUploadAllConfTest(){
        return Boolean.parseBoolean(System.getProperty(UPLOAD_ALL_CONF_TEST_PROPERTY, FALSE));
    }

}
