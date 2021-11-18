package com.salesforce.cte.testadvisor;

import java.awt.image.BufferedImage;
import java.io.File;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;

/**
 * @author Yibing Tao
 * This class provides methods to process screenshots
 */
public class ScreenshotManager {

    private ScreenshotManager(){
        //empty private constructor to hide default one and prevent instance
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
    public static ImageComparisonResult screenshotsComparison(File baseline, File current){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());

        //Create ImageComparison object and compare the images.
        return new ImageComparison(expectedImage, actualImage).compareImages();
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
    public static ImageComparisonResult screenshotsComparison(String baseline, String current){
        return screenshotsComparison(new File(baseline), new File(current));
    }

    /**
     * Compare 2 screenshots and saved the result image
     * @param baseline 
     * baseline screenshot
     * @param current
     * current screenshot
     * @param resultFileName
     * comparison result image
     * @return
     * image comparison result, including difference percentiage and list of diff area
     */
    public static ImageComparisonResult screenshotsComparison(File baseline, File current, String resultFileName){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());
    
        //Create ImageComparison object and compare the images.
        return new ImageComparison(expectedImage, actualImage,new File(resultFileName)).compareImages();
    }

}
