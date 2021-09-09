package com.salesforce.cte.datamodel.client;

/**
 * @author Yibing Tao
 * Defines test signal type name 
 * Each test signal enum value represents signals collected by a type of event listener
 */
public enum TestSignalEnum {
    AUTOMATION, //Test signal collected by Test Provider Listener
    SELENIUM    // Test signal collected by Selenium Event Listener
}
