package com.salesforce.cqe.adapter;

import java.io.InputStream;

import com.salesforce.cqe.helper.ProcessException;

/**
 * @author Yibing Tao
 * Adapter interface to process test result
 */
public interface IAdapter {
    public ITestRun process(InputStream input) throws ProcessException;
}
