package com.salesforce.cqe.helper;

/**
 * @auther Yibing Tao
 * DrillbitPortalException will throw when CLI request was rejected by
 * drillbit portal. The message will be the portal response.
 */
public class DrillbitPortalException extends Exception {
    public DrillbitPortalException(String message){
        super(message);
    }
}
