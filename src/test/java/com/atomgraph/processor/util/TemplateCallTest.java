/*
 * Copyright 2018 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.processor.util;

import com.atomgraph.processor.model.Template;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCallTest {
    
    public TemplateCallTest() {
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
     * Test of fromUri method, of class TemplateCall.
     */
    @Test
    public void testFromUri() {
        System.out.println("fromUri");
        String uri = "";
        Model model = null;
        Template template = null;
        TemplateCall expResult = null;
        TemplateCall result = TemplateCall.fromUri(uri, model, template);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fromResource method, of class TemplateCall.
     */
    @Test
    public void testFromResource() {
        System.out.println("fromResource");
        Resource resource = null;
        Template template = null;
        TemplateCall expResult = null;
        TemplateCall result = TemplateCall.fromResource(resource, template);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTemplate method, of class TemplateCall.
     */
    @Test
    public void testGetTemplate() {
        System.out.println("getTemplate");
        TemplateCall instance = null;
        Template expResult = null;
        Template result = instance.getTemplate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getURI method, of class TemplateCall.
     */
    @Test
    public void testGetURI() {
        System.out.println("getURI");
        TemplateCall instance = null;
        String expResult = "";
        String result = instance.getURI();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of applyArguments method, of class TemplateCall.
     */
    @Test
    public void testApplyArguments() {
        System.out.println("applyArguments");
        MultivaluedMap<String, String> queryParams = null;
        TemplateCall instance = null;
        TemplateCall expResult = null;
        TemplateCall result = instance.applyArguments(queryParams);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of applyDefaults method, of class TemplateCall.
     */
    @Test
    public void testApplyDefaults() {
        System.out.println("applyDefaults");
        TemplateCall instance = null;
        TemplateCall expResult = null;
        TemplateCall result = instance.applyDefaults();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of listArguments method, of class TemplateCall.
     */
    @Test
    public void testListArguments() {
        System.out.println("listArguments");
        TemplateCall instance = null;
        StmtIterator expResult = null;
        StmtIterator result = instance.listArguments();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hasArgument method, of class TemplateCall.
     */
    @Test
    public void testHasArgument_Property() {
        System.out.println("hasArgument");
        Property predicate = null;
        TemplateCall instance = null;
        boolean expResult = false;
        boolean result = instance.hasArgument(predicate);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getArgument method, of class TemplateCall.
     */
    @Test
    public void testGetArgument_Property() {
        System.out.println("getArgument");
        Property predicate = null;
        TemplateCall instance = null;
        Resource expResult = null;
        Resource result = instance.getArgument(predicate);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hasArgument method, of class TemplateCall.
     */
    @Test
    public void testHasArgument_String_RDFNode() {
        System.out.println("hasArgument");
        String varName = "";
        RDFNode object = null;
        TemplateCall instance = null;
        boolean expResult = false;
        boolean result = instance.hasArgument(varName, object);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getArgument method, of class TemplateCall.
     */
    @Test
    public void testGetArgument_String_RDFNode() {
        System.out.println("getArgument");
        String varName = "";
        RDFNode object = null;
        TemplateCall instance = null;
        Resource expResult = null;
        Resource result = instance.getArgument(varName, object);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getArgumentProperty method, of class TemplateCall.
     */
    @Test
    public void testGetArgumentProperty() {
        System.out.println("getArgumentProperty");
        Property predicate = null;
        TemplateCall instance = null;
        Statement expResult = null;
        Statement result = instance.getArgumentProperty(predicate);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of arg method, of class TemplateCall.
     */
    @Test
    public void testArg() {
        System.out.println("arg");
        Resource arg = null;
        TemplateCall instance = null;
        TemplateCall expResult = null;
        TemplateCall result = instance.arg(arg);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of validateOptionals method, of class TemplateCall.
     */
    @Test
    public void testValidateOptionals() {
        System.out.println("validateOptionals");
        TemplateCall instance = null;
        TemplateCall expResult = null;
        TemplateCall result = instance.validateOptionals();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQuerySolutionMap method, of class TemplateCall.
     */
    @Test
    public void testGetQuerySolutionMap() {
        System.out.println("getQuerySolutionMap");
        TemplateCall instance = null;
        QuerySolutionMap expResult = null;
        QuerySolutionMap result = instance.getQuerySolutionMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
