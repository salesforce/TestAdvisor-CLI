package com.salesforce.cqe.helper;

/**
 * @author Yibing Tao
 * ProcessException will throw when adapter failed to process the test result.
 * The cause will be the actual exception was throw.
 */
public class ProcessException extends Exception {
    public ProcessException(Throwable cause){
        super(cause);
    }
}
