/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.Rectangle;

/**
 * @author Yibing Tao
 * This class provides methods to process screenshots
 */
public class ScreenshotManager {

    private int minimalRectangleSize;
    public ScreenshotManager(){
        this.minimalRectangleSize = Configuration.getScreenshotMinDiffAreaSize();
    }

    /**
     * Compare 2 screenshots
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public ImageComparisonResult screenshotsComparison(File baseline, File current){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());

        //Create ImageComparison object and compare the images.
        ImageComparison imc =  new ImageComparison(expectedImage, actualImage);
        imc.setMinimalRectangleSize(minimalRectangleSize);
        return  imc.compareImages();
    }

    /**
     * Compare 2 screenshots
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @param excludedAreas
     * list of rectangle area to be excluded for image comparison on current screenshot
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public ImageComparisonResult screenshotsComparisonWithExcludedAreas(File baseline, File current, 
            List<Rectangle> excludedAreas){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());

        //Create ImageComparison object and compare the images.
        ImageComparison imc =  new ImageComparison(expectedImage, actualImage);
        imc.setExcludedAreas(excludedAreas);
        imc.setMinimalRectangleSize(minimalRectangleSize);
        return  imc.compareImages();
    }

    /**
     * Compare 2 screenshots
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public ImageComparisonResult screenshotsComparison(String baseline, String current){
        return screenshotsComparison(new File(baseline), new File(current));
    }

    /**
     * Compare 2 screenshots and saved the result image
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @param resultFile
     * comparison result image
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public ImageComparisonResult screenshotsComparison(File baseline, File current, File resultFile){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());
    
        //Create ImageComparison object and compare the images.
        ImageComparison imc = new ImageComparison(expectedImage, actualImage,resultFile);
        imc.setMinimalRectangleSize(minimalRectangleSize);
        return imc.compareImages();
    }

    /**
     * Compare 2 screenshots with excluded areas and saved the result image
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @param resultFile
     * comparison result image
     * @param excludedAreas
     * list of area in rectangle to exclude from comparisoin
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public ImageComparisonResult screenshotsComparisonWithExcludedAreas(File baseline, File current, 
        File resultFile, List<Rectangle> excludedAreas){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());
        
        //Create ImageComparison object and compare the images.
        ImageComparison imc = new ImageComparison(expectedImage, actualImage,resultFile);
        if (excludedAreas != null) imc.setExcludedAreas(excludedAreas);
        imc.setMinimalRectangleSize(minimalRectangleSize);
        return imc.compareImages();
    }


}
