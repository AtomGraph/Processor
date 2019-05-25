/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.processor.exception.ParameterException;
import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.impl.ParameterImpl;
import com.atomgraph.processor.model.impl.TemplateImpl;
import com.atomgraph.processor.vocabulary.LDT;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spinrdf.vocabulary.SPL;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCallTest
{
    
    private static final String PREDICATE1_LOCAL_NAME = "a", PREDICATE2_LOCAL_NAME = "b", UNUSED_PREDICATE_LOCAL_NAME = "unused";
    private static final String PARAM2_DEFAULT_VALUE = "default value";
    
    private Template template;
    private Property predicate1, predicate2, unusedPredicate;
    private Parameter param1, param2;
    private Resource resource;
    private TemplateCall call;
    
    @BeforeClass
    public static void setUpClass()
    {
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);
        BuiltinPersonalities.model.add(Parameter.class, ParameterImpl.factory);
    }
    
    @Before
    public void setUp()
    {
        Ontology ontology = ModelFactory.createOntologyModel().createOntology("http://test/ontology");
        
        predicate1 = ontology.getOntModel().createProperty("http://test/" + PREDICATE1_LOCAL_NAME);
        predicate2 = ontology.getOntModel().createProperty("http://test/" + PREDICATE2_LOCAL_NAME);
        unusedPredicate = ontology.getOntModel().createProperty("http://test/" + UNUSED_PREDICATE_LOCAL_NAME);
        
        param1 = ontology.getOntModel().createIndividual("http://test/ontology/param1", LDT.Parameter).
                addProperty(SPL.predicate, predicate1). // following SPIN convention to use local name
                addLiteral(SPL.optional, false).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Parameter.class);
        param2 = ontology.getOntModel().createIndividual("http://test/ontology/param2", LDT.Parameter).
                addProperty(SPL.predicate, predicate2). // following SPIN convention to use local name
                addLiteral(SPL.optional, false).
                addLiteral(SPL.defaultValue, PARAM2_DEFAULT_VALUE).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Parameter.class);

        template = ontology.getOntModel().createIndividual("http://test/ontology/template", LDT.Template).
                addLiteral(LDT.match, "{path}").
                addProperty(LDT.param, param1).
                addProperty(LDT.param, param2).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        
        resource = ModelFactory.createDefaultModel().createResource("http://resource/");
        call = TemplateCall.fromResource(resource, template);
    }
    
    @Test
    public void testApplyArguments()
    {
        String param1Value = "1", param2Value = "with space";
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add(PREDICATE1_LOCAL_NAME, param1Value);
        queryParams.add(PREDICATE2_LOCAL_NAME, param2Value);
        queryParams.add(UNUSED_PREDICATE_LOCAL_NAME, "X");
        
        String url = resource.getURI() + "?" + PREDICATE1_LOCAL_NAME + "=" + param1Value + "&" + PREDICATE2_LOCAL_NAME + "=" + param2Value.replace(" ", "%20");
        assertEquals(url, call.applyArguments(queryParams).build().getURI());
    }
    
    @Test
    public void testApplyDefaults()
    {
        String param1Value = "1";
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add(PREDICATE1_LOCAL_NAME, param1Value);
        // predicate2 value comes from spl:defaultValue instead
        queryParams.add(UNUSED_PREDICATE_LOCAL_NAME, "X");
        
        String url = resource.getURI() + "?" + PREDICATE1_LOCAL_NAME + "=" + param1Value + "&" + PREDICATE2_LOCAL_NAME + "=" + PARAM2_DEFAULT_VALUE.replace(" ", "%20");
        assertEquals(url, call.applyArguments(queryParams).applyDefaults().build().getURI());
    }
    
    @Test
    public void testGetArgument()
    {
        String param1Value = "1";
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add(PREDICATE1_LOCAL_NAME, param1Value);
        // predicate2 value comes from spl:defaultValue instead
        queryParams.add(UNUSED_PREDICATE_LOCAL_NAME, "X");
        
        TemplateCall applied = call.applyArguments(queryParams).applyDefaults();
        assertNotNull(applied.getArgument(predicate1));
        assertNotNull(applied.getArgument(predicate2));
        assertNull(applied.getArgument(unusedPredicate));
    }
    
    @Test(expected = ParameterException.class)
    public void testValidateOptionals()
    {
        String param2Value = "with space";
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        // parameter1 is mandatory (spl:defaultValue false), but its value is missing
        queryParams.add(PREDICATE2_LOCAL_NAME, param2Value);
        
        call.applyArguments(queryParams).validateOptionals();
    }
    
    @Test
    public void testValidateOptionalDefaults()
    {
        String param1Value = "1";
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add(PREDICATE1_LOCAL_NAME, param1Value);
        // parameter2 is mandatory (spl:defaultValue false) and its value is missing, but it has a default vaulue which applies
        
        TemplateCall applied = call.applyArguments(queryParams).applyDefaults().validateOptionals();
        assertNotNull(applied.getArgument(predicate2));
    }
    
}
