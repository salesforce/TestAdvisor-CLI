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
    public void testGetIsScreenshotComparisonEnabled(){
        System.clearProperty("testadvisor.screenshotcomparison");
        assertTrue(!Configuration.getIsScreenshotComparisonEnabled());

        System.setProperty("testadvisor.screenshotcomparison","true");
        assertTrue(Configuration.getIsScreenshotComparisonEnabled());

        System.setProperty("testadvisor.screenshotcomparison","false");
        assertTrue(!Configuration.getIsScreenshotComparisonEnabled());

        System.setProperty("testadvisor.screenshotcomparison","ok");
        assertTrue(!Configuration.getIsScreenshotComparisonEnabled());

        System.clearProperty("testadvisor.screenshotcomparison");
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

}
