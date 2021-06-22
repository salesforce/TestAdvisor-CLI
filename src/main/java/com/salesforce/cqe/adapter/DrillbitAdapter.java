package com.salesforce.cqe.adapter;

import java.io.InputStream;

import com.salesforce.cqe.helper.ProcessException;

/**
 * @author Yibing Tao
 * Adapter interface to process test result
 */
public interface DrillbitAdapter {
    public DrillbitTestRun process(InputStream input) throws ProcessException;
}
