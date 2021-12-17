/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

import org.junit.Test;

public class ScreenshotManagerTest {

    private ScreenshotManager screenshotManager = new ScreenshotManager();
    @Test
    public void testScreenshotComparisonSame() throws URISyntaxException{
        URL baselineUrl = getClass().getClassLoader().getResource("image/login.png");
        URL currentUrl = getClass().getClassLoader().getResource("image/login.png");
        File baselineFile = new File(baselineUrl.toURI());
        File currentFile = new File(currentUrl.toURI());
        ImageComparisonResult result =  screenshotManager.screenshotsComparison(baselineFile, currentFile);
        assertEquals(ImageComparisonState.MATCH, result.getImageComparisonState());
    }

    @Test
    public void testScreenshotComparisonMismatch() throws URISyntaxException{
        URL baselineUrl = getClass().getClassLoader().getResource("image/login.png");
        URL currentUrl = getClass().getClassLoader().getResource("image/login2.png");
        File baselineFile = new File(baselineUrl.toURI());
        File currentFile = new File(currentUrl.toURI());
        ImageComparisonResult result =  screenshotManager.screenshotsComparison(baselineFile, currentFile);
        assertEquals(ImageComparisonState.MISMATCH, result.getImageComparisonState());
        assertTrue(result.getDifferencePercent()>0);
        assertEquals(1, result.getRectangles().size());
    }

    @Test
    public void testScreenshotComparisonSave() throws URISyntaxException, IOException{
        URL baselineUrl = getClass().getClassLoader().getResource("image/login.png");
        URL currentUrl = getClass().getClassLoader().getResource("image/login2.png");
        File baselineFile = new File(baselineUrl.toURI());
        File currentFile = new File(currentUrl.toURI());
        File resultFile = new File("result.png");
        ImageComparisonResult result =  screenshotManager.screenshotsComparison(baselineFile, currentFile,resultFile);
        assertEquals(ImageComparisonState.MISMATCH, result.getImageComparisonState());
        assertTrue(Files.exists(resultFile.toPath()));
        Files.delete(resultFile.toPath());
    }
    
}
