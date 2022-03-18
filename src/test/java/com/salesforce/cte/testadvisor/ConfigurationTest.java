/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;

import org.junit.Test;

public class ConfigurationTest {
    
    @Test
    public void testGetIsSeleniumUrlEnabled(){
        System.clearProperty("testadvisor.selenium.url");
        assertTrue(!Configuration.getIsSeleniumUrlEnabled());

        System.setProperty("testadvisor.selenium.url","true");
        assertTrue(Configuration.getIsSeleniumUrlEnabled());

        System.setProperty("testadvisor.selenium.url","false");
        assertTrue(!Configuration.getIsSeleniumUrlEnabled());

        System.setProperty("testadvisor.selenium.url","ok");
        assertTrue(!Configuration.getIsSeleniumUrlEnabled());

        System.clearProperty("testadvisor.selenium.url");
    }

    @Test
    public void testGetIsSeleniumExceptionEnabled(){
        System.clearProperty("testadvisor.selenium.exception");
        assertTrue(!Configuration.getIsSeleniumExceptionEnabled());

        System.setProperty("testadvisor.selenium.exception","true");
        assertTrue(Configuration.getIsSeleniumExceptionEnabled());

        System.setProperty("testadvisor.selenium.exception","false");
        assertTrue(!Configuration.getIsSeleniumExceptionEnabled());

        System.setProperty("testadvisor.selenium.exception","ok");
        assertTrue(!Configuration.getIsSeleniumExceptionEnabled());

        System.clearProperty("testadvisor.selenium.exception");
    }

    @Test
    public void testGetIsSeleniumErrorMessageEnabled(){
        System.clearProperty("testadvisor.selenium.errormessage");
        assertTrue(!Configuration.getIsSeleniumErrorMessageEnabled());

        System.setProperty("testadvisor.selenium.errormessage","true");
        assertTrue(Configuration.getIsSeleniumErrorMessageEnabled());

        System.setProperty("testadvisor.selenium.errormessage","false");
        assertTrue(!Configuration.getIsSeleniumErrorMessageEnabled());

        System.setProperty("testadvisor.selenium.errormessage","ok");
        assertTrue(!Configuration.getIsSeleniumErrorMessageEnabled());

        System.clearProperty("testadvisor.selenium.errormessage");
    }

    @Test
    public void testGetIsSeleniumScreenshotEnabled(){
        System.clearProperty("testadvisor.selenium.screenshot");
        assertTrue(!Configuration.getIsSeleniumScreenshotEnabled());

        System.setProperty("testadvisor.selenium.screenshot","true");
        assertTrue(Configuration.getIsSeleniumScreenshotEnabled());

        System.setProperty("testadvisor.selenium.screenshot","false");
        assertTrue(!Configuration.getIsSeleniumScreenshotEnabled());

        System.setProperty("testadvisor.selenium.screenshot","ok");
        assertTrue(!Configuration.getIsSeleniumScreenshotEnabled());

        System.clearProperty("testadvisor.selenium.screenshot");
    }

    @Test
    public void testGetSignalLevel(){
        //Supported Level, OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
        System.clearProperty("testadvisor.signallevel");
        assertEquals(Level.WARNING, Configuration.getSignalLevel());

        System.setProperty("testadvisor.signallevel","OFF");
        assertEquals(Level.OFF, Configuration.getSignalLevel());

        System.setProperty("testadvisor.signallevel","SEVERE");
        assertEquals(Level.SEVERE, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","WARNING");
        assertEquals(Level.WARNING, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","INFO");
        assertEquals(Level.INFO, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","CONFIG");
        assertEquals(Level.CONFIG, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","FINE");
        assertEquals(Level.FINE, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","FINER");
        assertEquals(Level.FINER, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","FINEST");
        assertEquals(Level.FINEST, Configuration.getSignalLevel());
        
        System.setProperty("testadvisor.signallevel","ALL");
        assertEquals(Level.ALL, Configuration.getSignalLevel());
    
        System.clearProperty("testadvisor.signallevel");
    }

    @Test
    public void testGetExportScreenshotDiffArea(){
        System.clearProperty("testadvisor.exportscreenshotdiffarea");
        assertTrue(!Configuration.getExportScreenshotDiffArea());
 
        System.setProperty("testadvisor.exportscreenshotdiffarea","true");
        assertTrue(Configuration.getExportScreenshotDiffArea());

        System.setProperty("testadvisor.exportscreenshotdiffarea","false");
        assertTrue(!Configuration.getExportScreenshotDiffArea());

        System.setProperty("testadvisor.exportscreenshotdiffarea","ok");
        assertTrue(!Configuration.getExportScreenshotDiffArea());

        System.clearProperty("testadvisor.exportscreenshotdiffarea");
    }

    @Test
    public void testGetScreenshotMinDiffAreaSize(){
        System.clearProperty("testadvisor.screenshotmindiffareasize");
        assertEquals(20,Configuration.getScreenshotMinDiffAreaSize());

        System.setProperty("testadvisor.screenshotmindiffareasize","100");
        assertEquals(100,Configuration.getScreenshotMinDiffAreaSize());

        System.setProperty("testadvisor.screenshotmindiffareasize","invalid");
        assertEquals(20,Configuration.getScreenshotMinDiffAreaSize());

        System.clearProperty("testadvisor.screenshotmindiffareasize");
    }

    @Test
    public void testGetExportScreenshotDiffImage(){
        System.clearProperty("testadvisor.exportscreenshotdiffimage");
        assertEquals(false,Configuration.getExportScreenshotDiffImage());

        System.setProperty("testadvisor.exportscreenshotdiffimage","true");
        assertEquals(true,Configuration.getExportScreenshotDiffImage());

        System.setProperty("testadvisor.exportscreenshotdiffimage","false");
        assertEquals(false,Configuration.getExportScreenshotDiffImage());

        System.clearProperty("testadvisor.exportscreenshotdiffimage");
    }

    @Test
    public void testGetScreenshotMinDiffRatio(){
        System.clearProperty("testadvisor.screenshotmindiffratio");
        assertEquals(1,Configuration.getScreenshotMinDiffRatio());

        System.setProperty("testadvisor.screenshotmindiffratio","5");
        assertEquals(5,Configuration.getScreenshotMinDiffRatio());

        System.setProperty("testadvisor.screenshotmindiffratio","invalid");
        assertEquals(1,Configuration.getScreenshotMinDiffRatio());

        System.clearProperty("testadvisor.screenshotmindiffratio");
    }

}
