/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rappsilber.xlmod;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lfischer
 */
public class XLMODTest {
    XLModEntry expResult;
    
    public XLMODTest() {
        try {
            this.expResult = new XLModEntry("XLMOD:01000", "hydrolyzed BS3", 156.07864431,"(K,S,T,Y,Protein N-term)" , 1);
        } catch (ParseException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of read method, of class XLMOD.
     */
    @Test
    public void testRead_0args() throws Exception {
        System.out.println("read");
        XLMOD instance = new XLMOD();
        instance.read();
        // TODO review the generated test code and remove the default call to fail.
    }

//    /**
//     * Test of read method, of class XLMOD.
//     */
//    @Test
//    public void testRead_String() throws Exception {
//        System.out.println("read");
//        String source = "";
//        XLMOD instance = new XLMOD();
//        instance.read(source);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readCSV method, of class XLMOD.
//     */
//    @Test
//    public void testReadCSV() throws Exception {
//        System.out.println("readCSV");
//        String source = "";
//        XLMOD instance = new XLMOD();
//        instance.readCSV(source);
//        // TODO review the generated test code and remove the default call to fail.
//    }

//    /**
//     * Test of readObo method, of class XLMOD.
//     */
//    @Test
//    public void testReadObo() throws Exception {
//        System.out.println("readObo");
//        String source = "";
//        XLMOD instance = new XLMOD();
//        instance.readObo(source);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of add method, of class XLMOD.
//     */
//    @Test
//    public void testAdd() {
//        System.out.println("add");
//        XLModEntry entry = null;
//        XLMOD instance = new XLMOD();
//        boolean expResult = false;
//        boolean result = instance.add(entry);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of matches method, of class XLMOD.
     */
    @Test
    public void testMatches() {
        System.out.println("matches");
        XLModSpecificity s = new XLModSpecificity("K", false, false, false, false);
        String linkedResidues = "K";
        boolean isTerm = false;
        boolean isProtNterm = false;
        boolean isProtCTerm = false;
        boolean isPepNterm = false;
        boolean isPepCterm = false;
        XLMOD instance = new XLMOD();
        boolean expResult = true;
        boolean result = instance.matches(s, linkedResidues, isTerm, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
        assertEquals(expResult, result);
        s = new XLModSpecificity(XLModSpecificity.NON_SPECIFIC, false, false, false, false);
        result = instance.matches(s, linkedResidues, isTerm, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
        assertEquals(expResult, result);
        s = new XLModSpecificity("S", false, false, false, false);
        
        expResult = false;
        result = instance.matches(s, linkedResidues, isTerm, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
        assertEquals(expResult, result);

    }

//    /**
//     * Test of guessModification method, of class XLMOD.
//     */
//    @Test
//    public void testGuessModification_6args() {
//        System.out.println("guessModification");
//        double mass = 0.0;
//        String[] linkedResidues = null;
//        boolean[] isProtNterm = null;
//        boolean[] isProtCTerm = null;
//        boolean[] isPepNterm = null;
//        boolean[] isPepCterm = null;
//        XLMOD instance = new XLMOD();
//        XLModEntry result = instance.guessModification(mass, linkedResidues, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of guessModification method, of class XLMOD.
//     */
//    @Test
//    public void testGuessModification_7args() {
//        System.out.println("guessModification");
//        double mass = 0.0;
//        String name = "";
//        String[] linkedResidues = null;
//        boolean[] isProtNterm = null;
//        boolean[] isProtCTerm = null;
//        boolean[] isPepNterm = null;
//        boolean[] isPepCterm = null;
//        XLMOD instance = new XLMOD();
//        try {
//            instance.read();
//        } catch (IOException ex) {
//            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        XLModEntry result = instance.guessModification(mass, name, linkedResidues, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of guessModification method, of class XLMOD.
     */
    @Test
    public void testGuessModification_XLModQuery() {
        System.out.println("guessModification");
        boolean[] term = new boolean[]{false};
        XLModQuery q = new XLModQuery(156.07864431, "BS3", new String[]{"K"}, term, term, term, term);
        XLMOD instance = new XLMOD();
        try {
            instance.read();
        } catch (IOException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        XLModEntry result = instance.guessModification(q);
        assertEquals(expResult, result);
        
        q = new XLModQuery(156.07864431, new String[]{"K"}, term, term, term, term);
        result = instance.guessModification(q);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of guessModificationCached method, of class XLMOD.
     */
    @Test
    public void testGuessModificationCached() {
        System.out.println("guessModificationCached");
        boolean[] term = new boolean[]{false};
        XLModQuery q = new XLModQuery(156.07864431, "BS3", new String[]{"K"}, term, term, term, term);
        XLMOD instance = new XLMOD();
        try {
            instance.read();
        } catch (IOException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        XLModEntry result = instance.guessModificationCached(q);
        assertEquals(expResult, result);
        result = instance.guessModificationCached(q);
        assertEquals(expResult, result);
    }

    /**
     * Test of get method, of class XLMOD.
     */
    @Test
    public void testGet_String() {
        System.out.println("get");
        String id = "01000";
        XLMOD instance = new XLMOD();
        try {
            instance.read();
        } catch (IOException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        XLModEntry result = instance.get(id);
        assertEquals(expResult, result);
    }

    /**
     * Test of get method, of class XLMOD.
     */
    @Test
    public void testGet_int() {
        System.out.println("get");
        int id = 1000;
        XLMOD instance = new XLMOD();
        try {
            instance.read();
        } catch (IOException ex) {
            fail("IOException occured.");
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            fail("ParseException occured.");
        }
        XLModEntry result = instance.get(id);
        assertEquals(expResult, result);
    }

    /**
     * Test of iterator method, of class XLMOD.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");
        XLMOD instance = new XLMOD();
        try {
            instance.read();
        } catch (IOException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(XLMODTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Iterator<XLModEntry> result = instance.iterator();
    }

//    /**
//     * Test of getUsedURL method, of class XLMOD.
//     */
//    @Test
//    public void testGetUsedURL() {
//        System.out.println("getUsedURL");
//        XLMOD instance = new XLMOD();
//        String expResult = "";
//        String result = instance.getUsedURL();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

//    /**
//     * Test of setUsedURL method, of class XLMOD.
//     */
//    @Test
//    public void testSetUsedURL() {
//        System.out.println("setUsedURL");
//        String usedURL = "";
//        XLMOD instance = new XLMOD();
//        instance.setUsedURL(usedURL);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
