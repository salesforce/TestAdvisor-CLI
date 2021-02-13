package com.salesforce.bst.extension;

import java.util.List;

public class TestRunSignal {
    String testRunId;
    long   start;
    long   end;
    boolean hasAutomationChanged;
    boolean hasMetadataChanged;
    List<TestSignal> tests;
}
