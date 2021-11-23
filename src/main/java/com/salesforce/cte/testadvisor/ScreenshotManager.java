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
        ImageComparison imc =  new ImageComparison(expectedImage, actualImage);
        imc.setMinimalRectangleSize(5);
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
    public static ImageComparisonResult screenshotsComparisonWithExcludedAreas(File baseline, File current, 
            List<Rectangle> excludedAreas){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());

        //Create ImageComparison object and compare the images.
        ImageComparison imc =  new ImageComparison(expectedImage, actualImage);
        imc.setExcludedAreas(excludedAreas);
        imc.setMinimalRectangleSize(5);
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
    public static ImageComparisonResult screenshotsComparison(String baseline, String current){
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
    public static ImageComparisonResult screenshotsComparison(File baseline, File current, File resultFile){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());
    
        //Create ImageComparison object and compare the images.
        ImageComparison imc = new ImageComparison(expectedImage, actualImage,resultFile);
        imc.setMinimalRectangleSize(5);
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
    public static ImageComparisonResult screenshotsComparisonWithExcludedAreas(File baseline, File current, 
        File resultFile, List<Rectangle> excludedAreas){
        //load images to be compared:
        BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(baseline.getAbsolutePath());
        BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(current.getAbsolutePath());
        
        //Create ImageComparison object and compare the images.
        ImageComparison imc = new ImageComparison(expectedImage, actualImage,resultFile);
        imc.setExcludedAreas(excludedAreas);
        imc.setMinimalRectangleSize(5);
        return imc.compareImages();
    }


}
