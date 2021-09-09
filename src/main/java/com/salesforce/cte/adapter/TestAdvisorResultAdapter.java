package com.salesforce.cte.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestEvent;
import com.salesforce.cte.helper.ProcessException;

public class TestAdvisorResultAdapter implements TestAdvisorAdapter {

    @Override
    public TestAdvisorTestRun process(InputStream input) throws ProcessException {
        
        List<TestCaseExecution> testCaseExecutionList;
        try {
            testCaseExecutionList = new ObjectMapper()
                    .readValue(input,new TypeReference<List<TestCaseExecution>>(){});
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        
        List<TestAdvisorTestCase> testCaseList = new ArrayList<>();
        for(TestCaseExecution testExecution : testCaseExecutionList){
            List<TestAdvisorTestSignal> testSignalList = new ArrayList<>();
            for(TestEvent event : testExecution.eventList){
                if(event.eventLevel.equals("SEVERE") || event.eventLevel.equals("WARNING")){
                    //only collect severe and waring event
                    testSignalList.add(new TestSignalBase(event.getEventName(),
                                event.getEventContent(),getDatetime(event.startTime)));
                }
            }
            
            ZonedDateTime startTime = getDatetime(testExecution.startTime);
            ZonedDateTime endTime = getDatetime(testExecution.endTime);
            testCaseList.add(new TestCaseBase(testExecution.getTestName(),startTime,endTime,
                            testExecution.getTestStatus().toString(),testSignalList));
        }

        return new TestRunBase("","",ZonedDateTime.now(),ZonedDateTime.now(),testCaseList);
    }

    private ZonedDateTime getDatetime(String timestamp) {
        return ZonedDateTime.parse(timestamp,DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
}
