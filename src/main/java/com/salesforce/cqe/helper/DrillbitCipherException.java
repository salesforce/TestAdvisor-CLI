package com.salesforce.cqe.helper;

/**
 * @author Yibing Tao
 * This exception will throw when Drillbit CLI secrets manager encounter any
 * cihper related exception. The cause will be the actual exception was throw.
 */
public class DrillbitCipherException extends Exception {
    public DrillbitCipherException(Throwable cause){
        super(cause);
    }
}