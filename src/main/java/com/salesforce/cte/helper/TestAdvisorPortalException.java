package com.salesforce.cte.helper;

/**
 * @author Yibing Tao
 * TestAdvisorPortalException will throw when CLI request was rejected by
 * testadvisor portal. The message will be the portal response.
 */
public class TestAdvisorPortalException extends Exception {
    public TestAdvisorPortalException(String message){
        super(message);
    }
}
