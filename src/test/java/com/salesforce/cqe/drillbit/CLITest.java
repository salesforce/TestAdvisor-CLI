package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.salesforce.cqe.helper.DrillbitCipherException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

/**
 * @author Yibing Tao
 * Unit test for CLI class
 */
public class CLITest {

    @Test
    public void emptyTest() throws ParseException, IOException, DrillbitCipherException{
        CLI cli = new CLI(new String[0]);
        assertNull(cli.getCommand());
    }

    @Test
    public void helpTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-h"};
        
        try(ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os)){
            System.setOut(ps);
            CLI cli = new CLI(args);
            assertNull(cli.getCommand());
            assertTrue(os.toString().startsWith("usage:"));
        }
    }

    @Test
    public void versionTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-v"};
        
        try(ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os)){
            System.setOut(ps);
            CLI cli = new CLI(args);
            assertNull(cli.getCommand());
            assertTrue(os.toString().contains("version:"));
        }
    }

    @Test
    public void pathTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-n","abc"};
        CLI cli = new CLI(args);
        assertEquals("abc",cli.getResultFileName());
    }

    @Test
    public void forceTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-f"};
        CLI cli = new CLI(args);
        assertTrue(cli.isForce());
    }

    @Test
    public void setupTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-c","setup"};
        CLI cli = new CLI(args);
        assertEquals("SETUP",cli.getCommand());
    }

    @Test
    public void processTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-c","process"};
        CLI cli = new CLI(args);
        assertEquals("PROCESS",cli.getCommand());
    }

    @Test
    public void uploadTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-c","upload"};
        CLI cli = new CLI(args);
        assertEquals("UPLOAD",cli.getCommand());
    }

    @Test
    public void unsupportedTest() throws IOException, ParseException, DrillbitCipherException {
        String[] args = {"-c","unsupported"};
        CLI cli = new CLI(args);
        assertEquals("UNSUPPORTED",cli.getCommand());
    }

}
