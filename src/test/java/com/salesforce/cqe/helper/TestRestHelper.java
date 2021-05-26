/**
 * 
 */
package com.salesforce.cqe.helper;

import com.salesforce.cqe.helper.RestHelper;
import com.salesforce.cqe.helper.SecretsHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gneumann
 *
 */
public class TestRestHelper {
	//@Test
	public void testQueryAndCreateTestSuiteRecordsViaREST() {
		try {
			if (SecretsHelper.isSetupRequired())
				RestHelper.setupConnectionWithPortal();
			else
				RestHelper.connectToPortal();

			// query for up to 5 test suites
			JSONObject listOfTestSuites = RestHelper.query("/query?q=Select+Id,+Name+From+Test_Suite__c+Limit+5");
			JSONArray j = listOfTestSuites.getJSONArray("records");
			System.out.println("Found " + j.length() + " Test Suite records");
			for (int i = 0; i < j.length(); i++) {
				String testSuiteName = listOfTestSuites.getJSONArray("records").getJSONObject(i).getString("Name");
				String testSuiteId = listOfTestSuites.getJSONArray("records").getJSONObject(i).getString("Id");
				Assert.assertNotNull(testSuiteId);
				Assert.assertTrue(testSuiteId.length() > 0);
				Assert.assertNotNull(testSuiteName);
				Assert.assertTrue(testSuiteName.length() > 0);
			}
			// create the JSON object containing the new test suite details.
			JSONObject testSuite = new JSONObject();
			testSuite.put("Name", "DrillBit Test " + System.currentTimeMillis());

			// create new test suite
			JSONObject newTestSuiteResponse = RestHelper.create("/sobjects/Test_Suite__c/", testSuite);
			String newTestSuiteId = newTestSuiteResponse.getString("id");
			Assert.assertNotNull(newTestSuiteId);
			Assert.assertTrue(newTestSuiteId.length() > 0);
			System.out.println("Created Test Suite with ID " + newTestSuiteId);

			// close connection
			RestHelper.close();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
