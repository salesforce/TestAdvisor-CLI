package com.salesforce.cte.adapter;

import java.io.InputStream;

import com.salesforce.cte.helper.ProcessException;

/**
 * @author Yibing Tao
 * Adapter interface to process test result
 */
public interface TestAdvisorAdapter {
    public TestAdvisorTestRun process(InputStream input) throws ProcessException;
}
