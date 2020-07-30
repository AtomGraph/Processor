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
    private OntClass relativePathClass, absolutePathClass, thingClass, subClass, superClass, restrictedClass;
    private Model input;
    private Resource absolute, relative, thing, subInst, superInst, restricted;
    private final String absoluteId = "ABCDEFGHI", relativeId = "123456789", title = "With space", fragment = "something", restrictedId = "987654321";
    private final String restrictionValue = "http://restricted/";
    
    @Before
    public void setUp()
    {
        baseUriBuilder = UriBuilder.fromUri("http://base/");
        absolutePathBuilder = UriBuilder.fromUri("http://base/absolute/path");
        ontology = ModelFactory.createOntologyModel().createOntology("http://test/ontology");
        importedOntology = ontology.getOntModel().createOntology("http://test/ontology/import");
        ontology.addImport(importedOntology);
        skolemizer = new Skolemizer(ontology, baseUriBuilder, absolutePathBuilder);
        
        relativePathClass = ontology.getOntModel().createClass("http://test/ontology/relative-path-class");
        relativePathClass.addLiteral(LDT.path, "{identifier}");
        absolutePathClass = ontology.getOntModel().createClass("http://test/ontology/absolute-path-class");
        absolutePathClass.addLiteral(LDT.path, "/{identifier}");
        thingClass = ontology.getOntModel().createClass("http://test/ontology/thing-class");
        thingClass.addLiteral(LDT.path, "thing-{isPrimaryTopicOf.identifier}").
                addProperty(LDT.fragment, fragment);
        superClass = importedOntology.getOntModel().createClass("http://test/ontology/import/super-class");
        superClass.addLiteral(LDT.path, "super-{title}").
                addProperty(LDT.fragment, "super");
        subClass = ontology.getOntModel().createClass("http://test/ontology/sub-class");
        subClass.addProperty(RDFS.subClassOf, FOAF.Document).
                addProperty(RDFS.subClassOf, superClass);
        restrictedClass = ontology.getOntModel().createClass("http://test/ontology/restricted-class");
        restrictedClass.addLiteral(LDT.path, "{identifier}").
                addProperty(RDFS.subClassOf, restrictedClass.getOntModel().createHasValueRestriction(null, SIOC.HAS_CONTAINER, restrictedClass.getOntModel().createResource(restrictionValue)));
        restrictedClass.getOntModel().createResource(SIOC.HAS_CONTAINER.getURI()).
                addProperty(RDF.type, OWL.ObjectProperty);
        input = ModelFactory.createDefaultModel();
        absolute = input.createResource().
                addProperty(RDF.type, absolutePathClass).
                addLiteral(DCTerms.identifier, absoluteId);
        relative = input.createResource().
                addProperty(RDF.type, relativePathClass).
                addLiteral(DCTerms.identifier, relativeId);
        thing = input.createResource().
                addProperty(RDF.type, thingClass).
                addLiteral(DCTerms.title, title);
        subInst = input.createResource().
                addProperty(RDF.type, subClass).
                addLiteral(DCTerms.title, title);
        restricted = input.createResource().
                addProperty(RDF.type, restrictedClass).
                addLiteral(DCTerms.identifier, restrictedId);
        relative.addProperty(FOAF.primaryTopic, thing);
        thing.addProperty(FOAF.isPrimaryTopicOf, relative);
    }
    
    /**
     * Test of build method, of class Skolemizer.
     */
    @Test
    public void testBuild_Model()
    {
        Model expected = ModelFactory.createDefaultModel();
        Resource expAbsolute = expected.createResource(baseUriBuilder.clone().path(absoluteId).build().toString()).
                addProperty(RDF.type, absolutePathClass).
                addLiteral(DCTerms.identifier, absoluteId);
        Resource expRelative = expected.createResource(absolutePathBuilder.clone().path(relativeId).build().toString()).
                addProperty(RDF.type, relativePathClass).
                addLiteral(DCTerms.identifier, relativeId);
        Resource expThing = expected.createResource(absolutePathBuilder.clone().path("thing-" + relativeId).fragment(fragment).build().toString()).
                addProperty(RDF.type, thingClass).
                addLiteral(DCTerms.title, title);
        Resource expSub = expected.createResource(absolutePathBuilder.clone().path("super-" + title).fragment("super").build().toString()).
                addProperty(RDF.type, subClass).
                addLiteral(DCTerms.title, title);
        Resource expRestricted = expected.createResource(UriBuilder.fromUri(restrictionValue).clone().path(restrictedId).build().toString()).
                addProperty(RDF.type, restrictedClass).
                addLiteral(DCTerms.identifier, restrictedId);
        expRelative.addProperty(FOAF.primaryTopic, expThing);
        expThing.addProperty(FOAF.isPrimaryTopicOf, expRelative);
        
        Model result = skolemizer.build(input);
        assertTrue(result.isIsomorphicWith(expected));
    }

    /**
     * Test of build method, of class Skolemizer.
     */
    @Test
    public void testBuild_Resource_OntClass()
    {
        URI relativeResult = skolemizer.build(relative, relativePathClass);
        URI relativeExp = absolutePathBuilder.clone().path(relativeId).build();
        assertEquals(relativeExp, relativeResult);

        URI absoluteResult = skolemizer.build(absolute, absolutePathClass);
        URI absoluteExp = baseUriBuilder.clone().path(absoluteId).build();
        assertEquals(absoluteExp, absoluteResult);

        URI restrictedResult = skolemizer.build(restricted, restrictedClass);
        URI restrictedExp = UriBuilder.fromUri(restrictionValue).clone().path(restrictedId).build();
        assertEquals(restrictedExp, restrictedResult);
        
        URI thingResult = skolemizer.build(thing, thingClass);
        URI thingExp = absolutePathBuilder.clone().path("thing-" + relativeId).fragment(fragment).build();
        assertEquals(thingExp, thingResult);
    }
    
    @Test(expected = OntologyException.class)
    public void testBuild_Resource_OntClassInvalidPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid");
        OntClass invalidPathClass = invalidOntology.getOntModel().createClass("http://test/invalid/path-class");
        invalidPathClass.addLiteral(LDT.path, 123);
        Resource invalid = ModelFactory.createDefaultModel().createResource();
        
        URI invalidResult = skolemizer.build(invalid, invalidPathClass);
        URI invalidExp = absolutePathBuilder.clone().path(title).fragment(fragment).build();
        assertEquals(invalidExp, invalidResult);
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
    
    /**
     * Test of getStringValue method, of class Skolemizer.
     */
    @Test
    public void testGetStringValue()
    {
        String pathResult = skolemizer.getStringValue(relativePathClass, LDT.path);
        assertEquals("{identifier}", pathResult);
    }

    @Test
    public void testGetStringValueWithoutPath()
    {
        OntClass classWithoutPath = ModelFactory.createOntologyModel().createClass();
        String noPathResult = skolemizer.getStringValue(classWithoutPath, LDT.path);
        assertNull(noPathResult);
    }
    
    /**
     * Test of getStringValue method, of class Skolemizer.
     */
    @Test(expected = OntologyException.class)
    public void testGetStringValueWithNonLiteralPath()
    {
        OntClass nonLiteralPath = ModelFactory.createOntologyModel().createClass();
        nonLiteralPath.addProperty(LDT.path, nonLiteralPath.getOntModel().createResource());
        skolemizer.getStringValue(nonLiteralPath, LDT.path);
    }
    
    @Test(expected = OntologyException.class)
    public void testGetStringValueWithNumericalPath()
    {
        OntClass numericalPath = ModelFactory.createOntologyModel().createClass();
        numericalPath.addLiteral(LDT.path, 123);
        skolemizer.getStringValue(numericalPath, LDT.path);
    }
    
    /**
     * Test of getParent method, of class Skolemizer.
     */
    @Test
    public void testGetParent()
    {
        Resource restrictedResult = skolemizer.getParent(restrictedClass);
        assertEquals(input.createResource(restrictionValue), restrictedResult);
    }
    
}
