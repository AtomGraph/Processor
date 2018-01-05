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
package com.atomgraph.processor.model.impl;

import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.query.QueryBuilder;
import com.atomgraph.processor.update.UpdateBuilder;
import com.atomgraph.processor.vocabulary.LDT;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.CacheControl;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.spinrdf.model.Command;
import org.spinrdf.model.TemplateCall;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateImplTest {
    
    public static Personality<RDFNode> personality;
    public TemplateImpl instance;
    
    @BeforeClass
    public static void setUpClass()
    {
        personality = BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
        OntModel model = ModelFactory.createOntologyModel();
        Individual template = model.createIndividual(LDT.Template);
        String path = "/some/{path}";
        template.addLiteral(LDT.path, path);
        instance = new TemplateImpl(template.asNode(), new EnhGraph(model.getGraph(), personality));
    }

    /**
     * Test of getPath method, of class TemplateImpl.
     */
    @Test
    public void testGetValidPath()
    {

        UriTemplate expResult = new UriTemplate("/some/{path}");
        UriTemplate result = instance.getPath();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFragmentTemplate method, of class TemplateImpl.
     */
    @Test
    public void testGetFragmentTemplate() {
        System.out.println("getFragmentTemplate");
        TemplateImpl instance = null;
        String expResult = "";
        String result = instance.getFragmentTemplate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQuery method, of class TemplateImpl.
     */
    @Test
    public void testGetQuery() {
        System.out.println("getQuery");
        TemplateImpl instance = null;
        Resource expResult = null;
        Resource result = instance.getQuery();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUpdate method, of class TemplateImpl.
     */
    @Test
    public void testGetUpdate() {
        System.out.println("getUpdate");
        TemplateImpl instance = null;
        Resource expResult = null;
        Resource result = instance.getUpdate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPriority method, of class TemplateImpl.
     */
    @Test
    public void testGetPriority() {
        System.out.println("getPriority");
        TemplateImpl instance = null;
        Double expResult = null;
        Double result = instance.getPriority();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getParameters method, of class TemplateImpl.
     */
    @Test
    public void testGetParameters() {
        System.out.println("getParameters");
        TemplateImpl instance = null;
        Map<Property, Parameter> expResult = null;
        Map<Property, Parameter> result = instance.getParameters();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLocalParameters method, of class TemplateImpl.
     */
    @Test
    public void testGetLocalParameters() {
        System.out.println("getLocalParameters");
        TemplateImpl instance = null;
        Map<Property, Parameter> expResult = null;
        Map<Property, Parameter> result = instance.getLocalParameters();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addSuperParameters method, of class TemplateImpl.
     */
    @Test
    public void testAddSuperParameters() {
        System.out.println("addSuperParameters");
        Template template = null;
        Map<Property, Parameter> args = null;
        TemplateImpl instance = null;
        Map<Property, Parameter> expResult = null;
        Map<Property, Parameter> result = instance.addSuperParameters(template, args);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getParameterMap method, of class TemplateImpl.
     */
    @Test
    public void testGetParameterMap() {
        System.out.println("getParameterMap");
        TemplateImpl instance = null;
        Map<String, Parameter> expResult = null;
        Map<String, Parameter> result = instance.getParameterMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLanguages method, of class TemplateImpl.
     */
    @Test
    public void testGetLanguages_0args() {
        System.out.println("getLanguages");
        TemplateImpl instance = null;
        List<Locale> expResult = null;
        List<Locale> result = instance.getLanguages();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLanguages method, of class TemplateImpl.
     */
    @Test
    public void testGetLanguages_Property() {
        System.out.println("getLanguages");
        Property property = null;
        TemplateImpl instance = null;
        List<Locale> expResult = null;
        List<Locale> result = instance.getLanguages(property);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLoadClass method, of class TemplateImpl.
     */
    @Test
    public void testGetLoadClass() {
        System.out.println("getLoadClass");
        TemplateImpl instance = null;
        Resource expResult = null;
        Resource result = instance.getLoadClass();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCacheControl method, of class TemplateImpl.
     */
    @Test
    public void testGetCacheControl() {
        System.out.println("getCacheControl");
        TemplateImpl instance = null;
        CacheControl expResult = null;
        CacheControl result = instance.getCacheControl();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStringValue method, of class TemplateImpl.
     */
    @Test
    public void testGetStringValue() {
        System.out.println("getStringValue");
        Property property = null;
        TemplateImpl instance = null;
        String expResult = "";
        String result = instance.getStringValue(property);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQueryBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetQueryBuilder_URI() {
        System.out.println("getQueryBuilder");
        URI base = null;
        TemplateImpl instance = null;
        QueryBuilder expResult = null;
        QueryBuilder result = instance.getQueryBuilder(base);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQueryBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetQueryBuilder_URI_Model() {
        System.out.println("getQueryBuilder");
        URI base = null;
        Model commandModel = null;
        TemplateImpl instance = null;
        QueryBuilder expResult = null;
        QueryBuilder result = instance.getQueryBuilder(base, commandModel);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQueryBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetQueryBuilder_3args() {
        System.out.println("getQueryBuilder");
        Resource queryOrTemplateCall = null;
        URI base = null;
        Model commandModel = null;
        TemplateImpl instance = null;
        QueryBuilder expResult = null;
        QueryBuilder result = instance.getQueryBuilder(queryOrTemplateCall, base, commandModel);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getParameterizedSparqlString method, of class TemplateImpl.
     */
    @Test
    public void testGetParameterizedSparqlString_TemplateCall_URI() {
        System.out.println("getParameterizedSparqlString");
        TemplateCall spinTemplateCall = null;
        URI base = null;
        TemplateImpl instance = null;
        ParameterizedSparqlString expResult = null;
        ParameterizedSparqlString result = instance.getParameterizedSparqlString(spinTemplateCall, base);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getParameterizedSparqlString method, of class TemplateImpl.
     */
    @Test
    public void testGetParameterizedSparqlString_Command_URI() {
        System.out.println("getParameterizedSparqlString");
        Command command = null;
        URI base = null;
        TemplateImpl instance = null;
        ParameterizedSparqlString expResult = null;
        ParameterizedSparqlString result = instance.getParameterizedSparqlString(command, base);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUpdateBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetUpdateBuilder_URI() {
        System.out.println("getUpdateBuilder");
        URI base = null;
        TemplateImpl instance = null;
        UpdateBuilder expResult = null;
        UpdateBuilder result = instance.getUpdateBuilder(base);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUpdateBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetUpdateBuilder_URI_Model() {
        System.out.println("getUpdateBuilder");
        URI base = null;
        Model commandModel = null;
        TemplateImpl instance = null;
        UpdateBuilder expResult = null;
        UpdateBuilder result = instance.getUpdateBuilder(base, commandModel);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUpdateBuilder method, of class TemplateImpl.
     */
    @Test
    public void testGetUpdateBuilder_3args() {
        System.out.println("getUpdateBuilder");
        Resource updateOrTemplateCall = null;
        URI base = null;
        Model commandModel = null;
        TemplateImpl instance = null;
        UpdateBuilder expResult = null;
        UpdateBuilder result = instance.getUpdateBuilder(updateOrTemplateCall, base, commandModel);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class TemplateImpl.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        TemplateImpl instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hasSuperTemplate method, of class TemplateImpl.
     */
    @Test
    public void testHasSuperTemplate() {
        System.out.println("hasSuperTemplate");
        Template superTemplate = null;
        TemplateImpl instance = null;
        boolean expResult = false;
        boolean result = instance.hasSuperTemplate(superTemplate);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
