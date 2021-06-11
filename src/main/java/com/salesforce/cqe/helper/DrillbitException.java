package com.salesforce.cqe.helper;

/**
 * @auther Yibing Tao
 * Drillbit Exception will throw when CLI payload was rejected by
 * drillbit portal. The message will be the rejection reason.
 */
public class DrillbitException extends Exception {
    public DrillbitException(String message){
        super(message);
    }
}
