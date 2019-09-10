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
import com.atomgraph.processor.util.Skolemizer.ClassPrecedence;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.SIOC;
import com.sun.jersey.api.uri.UriTemplateParser;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
    private OntClass relativePathClass, absolutePathClass, thingClass, importedClass, restrictedClass, undefinedClass;
    private Model input;
    private Resource absolute, relative, thing, imported, restricted;
    private final String absoluteId = "ABCDEFGHI", relativeId = "123456789", thingTitle = "With space", thingFragment = "something", restrictedId = "987654321";
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
        relativePathClass.addLiteral(LDT.path, "{identifier}").
                addProperty(RDFS.isDefinedBy, ontology);
        absolutePathClass = ontology.getOntModel().createClass("http://test/ontology/absolute-path-class");
        absolutePathClass.addLiteral(LDT.path, "/{identifier}").
                addProperty(RDFS.isDefinedBy, ontology);
        thingClass = ontology.getOntModel().createClass("http://test/ontology/thing-class");
        thingClass.addLiteral(LDT.path, "{title}").
                addProperty(LDT.fragment, "something").
                addProperty(RDFS.isDefinedBy, ontology);
        importedClass = importedOntology.getOntModel().createClass("http://test/ontology/import/thing-class");
        importedClass.addLiteral(LDT.path, "{title}").
                addProperty(LDT.fragment, "something").
                addProperty(RDFS.isDefinedBy, importedOntology);
        restrictedClass = ontology.getOntModel().createClass("http://test/ontology/restricted-class");
        restrictedClass.addLiteral(LDT.path, "{identifier}").
                addProperty(RDFS.isDefinedBy, ontology).
                addProperty(RDFS.subClassOf, restrictedClass.getOntModel().createHasValueRestriction(null, SIOC.HAS_CONTAINER, restrictedClass.getOntModel().createResource(restrictionValue)));
        restrictedClass.getOntModel().createResource(SIOC.HAS_CONTAINER.getURI()).
                addProperty(RDF.type, OWL.ObjectProperty);
        undefinedClass = ontology.getOntModel().createClass("http://test/ontology/undefined-class");
        undefinedClass.addLiteral(LDT.path, "{whateverest}"); // does not have rdfs:isDefinedBy
                
        input = ModelFactory.createDefaultModel();
        absolute = input.createResource().
                addProperty(RDF.type, absolutePathClass).
                addLiteral(DCTerms.identifier, absoluteId);
        relative = input.createResource().
                addProperty(RDF.type, relativePathClass).
                addLiteral(DCTerms.identifier, relativeId);
        thing = input.createResource().
                addProperty(RDF.type, thingClass).
                addLiteral(DCTerms.title, thingTitle);
        imported = input.createResource().
                addProperty(RDF.type, importedClass).
                addLiteral(DCTerms.title, thingTitle);
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
        Resource expThing = expected.createResource(absolutePathBuilder.clone().path(thingTitle).fragment(thingFragment).build().toString()).
                addProperty(RDF.type, thingClass).
                addLiteral(DCTerms.title, thingTitle);
        Resource expImported = expected.createResource(absolutePathBuilder.clone().path(thingTitle).fragment(thingFragment).build().toString()).
                addProperty(RDF.type, importedClass).
                addLiteral(DCTerms.title, thingTitle);
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
        URI thingExp = absolutePathBuilder.clone().path(thingTitle).fragment(thingFragment).build();
        assertEquals(thingExp, thingResult);
    }
    
    @Test(expected = OntologyException.class)
    public void testBuild_Resource_OntClassInvalidPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid");
        OntClass invalidPathClass = invalidOntology.getOntModel().createClass("http://test/invalid/path-class");
        invalidPathClass.addLiteral(LDT.path, 123).
                addProperty(RDFS.isDefinedBy, invalidOntology);
        Resource invalid = ModelFactory.createDefaultModel().createResource();
        
        URI invalidResult = skolemizer.build(invalid, invalidPathClass);
        URI invalidExp = absolutePathBuilder.clone().path(thingTitle).fragment(thingFragment).build();
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
     * Test of match method, of class Skolemizer.
     */
    @Test
    public void testMatch()
    {
        SortedSet<Skolemizer.ClassPrecedence> relativeExp = new TreeSet<>();
        relativeExp.add(new ClassPrecedence(relativePathClass, 0));
        SortedSet<Skolemizer.ClassPrecedence> relativeResult = skolemizer.match(ontology, relative, RDF.type, 0);
        assertEquals(relativeExp, relativeResult);
        
        SortedSet<Skolemizer.ClassPrecedence> absoluteExp = new TreeSet<>();
        absoluteExp.add(new ClassPrecedence(absolutePathClass, 0));
        SortedSet<Skolemizer.ClassPrecedence> absoluteResult = skolemizer.match(ontology, absolute, RDF.type, 0);
        assertEquals(absoluteExp, absoluteResult);
        
        SortedSet<Skolemizer.ClassPrecedence> thingExp = new TreeSet<>();
        thingExp.add(new ClassPrecedence(thingClass, 0));
        SortedSet<Skolemizer.ClassPrecedence> thingResult = skolemizer.match(ontology, thing, RDF.type, 0);
        assertEquals(thingExp, thingResult);
        
        SortedSet<Skolemizer.ClassPrecedence> importedExp = new TreeSet<>();
        importedExp.add(new ClassPrecedence(importedClass, -1)); // import is minus level
        SortedSet<Skolemizer.ClassPrecedence> importedResult = skolemizer.match(ontology, imported, RDF.type, 0);
        assertEquals(importedExp, importedResult);
        
        SortedSet<Skolemizer.ClassPrecedence> relativeImportedExp = new TreeSet<>();
        SortedSet<Skolemizer.ClassPrecedence> relativeImportedResult = skolemizer.match(importedOntology, relative, RDF.type, 0);
        assertEquals(relativeImportedExp, relativeImportedResult);
    }
    
    /**
     * Test of getStringValue method, of class Skolemizer.
     */
    @Test
    public void testGetStringValue()
    {
        String pathResult = skolemizer.getStringValue(thingClass, LDT.path);
        assertEquals("{title}", pathResult);
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
