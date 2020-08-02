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

import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.SIOC;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.apache.jena.ontology.AllValuesFromRestriction;
import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.glassfish.jersey.uri.internal.UriTemplateParser;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SkolemizerTest
{
        
    private UriBuilder baseUriBuilder, absolutePathBuilder;
    private Ontology ontology, importedOntology;
    private Skolemizer skolemizer;
    
    @Before
    public void setUp()
    {
        baseUriBuilder = UriBuilder.fromUri("http://base/");
        absolutePathBuilder = UriBuilder.fromUri("http://base/absolute/path");
        ontology = ModelFactory.createOntologyModel().createOntology("http://test/ontology");
        importedOntology = ontology.getOntModel().createOntology("http://test/ontology/import");
        ontology.addImport(importedOntology);
        skolemizer = new Skolemizer(ontology, baseUriBuilder, absolutePathBuilder);
    }

    @Test
    public void testInheritance()
    {
        OntClass superClass = importedOntology.getOntModel().createClass();
        superClass.addLiteral(LDT.path, "super-{title}");
        
        OntClass cls = ontology.getOntModel().createClass();
        cls.addProperty(RDFS.subClassOf, FOAF.Document).
                addProperty(RDFS.subClassOf, superClass);
     
        String title = "Whateverest";
        Resource subInst = ModelFactory.createDefaultModel().
                createResource().
                addProperty(RDF.type, cls).
                addLiteral(DCTerms.title, title);
        
        URI expected = absolutePathBuilder.clone().path("super-" + title).build();
        URI actual = skolemizer.build(subInst);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testAbsolutePath()
    {
        OntClass cls = ontology.getOntModel().createClass();
        cls.addLiteral(LDT.path, "/{identifier}");

        String id = "ABCDEFGHI";
        Resource inst = ModelFactory.createDefaultModel().
            createResource().
            addProperty(RDF.type, cls).
            addLiteral(DCTerms.identifier, id);

        URI expected = baseUriBuilder.clone().path(id).build();
        URI actual = skolemizer.build(inst);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testRelativePath()
    {
        OntClass cls = ontology.getOntModel().createClass();
        cls.addLiteral(LDT.path, "{identifier}");

        String id = "ABCDEFGHI";
        Resource inst = ModelFactory.createDefaultModel().
                createResource().
                addProperty(RDF.type, cls).
                addLiteral(DCTerms.identifier, id);
        
        URI expected = absolutePathBuilder.clone().path(id).build();
        URI actual = skolemizer.build(inst);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testPrimaryTopic()
    {
        String id = "123456789";
        OntClass cls = ontology.getOntModel().createClass();
        cls.addLiteral(LDT.path, "thing-{isPrimaryTopicOf.identifier}");

        Model model = ModelFactory.createDefaultModel();
        Resource doc = model.createResource().
                addLiteral(DCTerms.identifier, id);
        Resource thing = model.createResource().
                addProperty(RDF.type, cls).
                addProperty(FOAF.isPrimaryTopicOf, doc);
        
        URI expected = absolutePathBuilder.clone().path("thing-" + id).build();
        URI actual = skolemizer.build(thing);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testHasValueRestrictionParent()
    {
        String hasValue = "http://restricted/";
                
        HasValueRestriction hvr = ontology.getOntModel().
                createHasValueRestriction(null, SIOC.HAS_CONTAINER, ontology.getOntModel().
                        createResource(hasValue));
        // use inheritance as well
        OntClass superCls = ontology.getOntModel().createClass();
        superCls.addLiteral(LDT.path, "hv-{identifier}");
        OntClass cls = ontology.getOntModel().createClass();
        cls.addProperty(RDFS.subClassOf, hvr).
                addProperty(RDFS.subClassOf, superCls);
        // sioc:has_container has to be an owl:ObjectProperty, otherwise we'll get ConversionException
        ontology.getOntModel().createResource(SIOC.HAS_CONTAINER.getURI()).
                addProperty(RDF.type, OWL.ObjectProperty);

        String id = "987654321";
        Resource inst = ModelFactory.createDefaultModel().
                createResource().
                addProperty(RDF.type, cls).
                addLiteral(DCTerms.identifier, id);

        URI expected = UriBuilder.fromUri(hasValue).path("hv-" + id).build();
        URI actual = skolemizer.build(inst);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testAllValuesFromRestrictionParent()
    {
        String hasValue = "http://restricted/";

        HasValueRestriction hvr = ontology.getOntModel().
                createHasValueRestriction(null, SIOC.HAS_CONTAINER, ontology.getOntModel().
                        createResource(hasValue));

        OntClass hvrCls = ontology.getOntModel().createClass();
        hvrCls.addProperty(RDFS.subClassOf, hvr);
        // sioc:has_container has to be an owl:ObjectProperty, otherwise we'll get ConversionException
        ontology.getOntModel().createResource(SIOC.HAS_CONTAINER.getURI()).
                addProperty(RDF.type, OWL.ObjectProperty);

        AllValuesFromRestriction avfr = ontology.getOntModel().
                createAllValuesFromRestriction(null, FOAF.primaryTopic, hvrCls);
        // use inheritance as well
        OntClass superCls = ontology.getOntModel().createClass();
        superCls.addLiteral(LDT.path, "avf-{isPrimaryTopicOf.identifier}");
        OntClass cls = ontology.getOntModel().createClass();
        cls.addProperty(RDFS.subClassOf, avfr).
                addProperty(RDFS.subClassOf, superCls);

        String id = "987654321";
        Model model = ModelFactory.createDefaultModel();
        Resource doc = model.createResource().
                addLiteral(DCTerms.identifier, id);
        Resource thing = model.createResource().
                addProperty(RDF.type, cls).
                addProperty(FOAF.isPrimaryTopicOf, doc);

        URI expected = UriBuilder.fromUri(hasValue).path("avf-" + id).build();
        URI actual = skolemizer.build(thing);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testFragment()
    {
        String fragment = "something";
        // use inheritance as well
        OntClass superCls = ontology.getOntModel().createClass();
        superCls.addLiteral(LDT.fragment, fragment);
        OntClass cls = ontology.getOntModel().createClass();
        cls.addLiteral(LDT.path, "{identifier}").
                addProperty(RDFS.subClassOf, superCls);

        String id = "ABCDEFGHI";
        Resource inst = ModelFactory.createDefaultModel().
                createResource().
                addProperty(RDF.type, cls).
                addLiteral(DCTerms.identifier, id);
        
        URI expected = absolutePathBuilder.clone().path(id).fragment(fragment).build();
        URI actual = skolemizer.build(inst);
        assertEquals(expected, actual);
    }
    
    @Test(expected = OntologyException.class)
    public void testInvalidPath()
    {
        OntClass cls = ontology.getOntModel().createClass();
        cls.addLiteral(LDT.path, 123);
        Resource invalid = ModelFactory.createDefaultModel().
                createResource().
                addProperty(RDF.type, cls);
        
        skolemizer.build(invalid);
    }
    /**
     * Test of getNameValueMap method, of class Skolemizer.
     */
    @Test
    public void testGetNameValueMap()
    {
        UriTemplateParser parser = new UriTemplateParser("{name}/{title}|{smth}|{primaryTopic.title}");
        Model model = ModelFactory.createDefaultModel();
        Literal secondTitle = model.createLiteral("Second");
        Literal firstTitle = model.createLiteral("First");

        Resource second = model.createResource().
                addLiteral(DCTerms.title, secondTitle);
        Resource first = model.createResource().
                addLiteral(DCTerms.title, firstTitle).
                addProperty(FOAF.primaryTopic, second);
        
        Map<String, String> expected = new HashMap<>();
        expected.put("title", firstTitle.getString());
        expected.put("primaryTopic.title", secondTitle.getString());
        
        Map<String, String> result = skolemizer.getNameValueMap(first, parser);
        assertEquals(expected, result);
    }

    /**
     * Test of getLiteral method, of class Skolemizer.
     */
    @Test
    public void testGetLiteral()
    {
        Model model = ModelFactory.createDefaultModel();
        Literal secondTitle = model.createLiteral("Second");
        Literal firstTitle = model.createLiteral("First");

        Resource second = model.createResource().
                addLiteral(DCTerms.title, secondTitle);
        Resource first = model.createResource().
                addLiteral(DCTerms.title, firstTitle).
                addProperty(FOAF.primaryTopic, second);
        
        Literal firstResult = skolemizer.getLiteral(first, "title");
        assertEquals(firstTitle, firstResult);
        Literal secondResult = skolemizer.getLiteral(first, "primaryTopic.title");
        assertEquals(secondTitle, secondResult);
        Literal resultFail1 = skolemizer.getLiteral(first, "primaryTopic");
        assertNull(resultFail1); // primaryTopic is a resource, not a literal
        Literal resultFail2 = skolemizer.getLiteral(first, "whatever");
        assertNull(resultFail2); // no such property
    }

    /**
     * Test of getResource method, of class Skolemizer.
     */
    @Test
    public void testGetResource()
    {
        Model model = ModelFactory.createDefaultModel();

        Resource second = model.createResource().
                addLiteral(DCTerms.title, "Second");
        Resource first = model.createResource().
                addLiteral(DCTerms.title, "First").
                addProperty(FOAF.primaryTopic, second);
        
        Resource secondResult = skolemizer.getResource(first, "primaryTopic");
        assertEquals(second, secondResult);
        Resource resultFail1 = skolemizer.getResource(first, "title");
        assertNull(resultFail1); // title is a literal, not a resource
        Resource resultFail2 = skolemizer.getResource(first, "primaryTopic.title");
        assertNull(resultFail2); // title is a literal, not a resource
        Resource resultFail3 = skolemizer.getResource(first, "whatever");
        assertNull(resultFail3); // no such property
    }
    
}
