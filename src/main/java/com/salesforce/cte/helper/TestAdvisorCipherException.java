package com.salesforce.cte.helper;

/**
 * @author Yibing Tao
 * This exception will throw when TestAdvisor CLI secrets manager encounter any
 * cihper related exception. The cause will be the actual exception was throw.
 */
public class TestAdvisorCipherException extends Exception {
    public TestAdvisorCipherException(Throwable cause){
        super(cause);
    }
}