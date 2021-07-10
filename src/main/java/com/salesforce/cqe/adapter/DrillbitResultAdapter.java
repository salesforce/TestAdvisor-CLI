package com.salesforce.cqe.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.cqe.admin.TestCaseExecution;
import com.salesforce.cqe.admin.TestEvent;
import com.salesforce.cqe.helper.ProcessException;

public class DrillbitResultAdapter implements DrillbitAdapter {

    @Override
    public DrillbitTestRun process(InputStream input) throws ProcessException {
        
        List<TestCaseExecution> testCaseExecutionList;
        try {
            testCaseExecutionList = new ObjectMapper()
                    .readValue(input,new TypeReference<List<TestCaseExecution>>(){});
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        
        List<DrillbitTestCase> testCaseList = new ArrayList<>();
        for(TestCaseExecution testExecution : testCaseExecutionList){
            List<DrillbitTestSignal> testSignalList = new ArrayList<>();
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
