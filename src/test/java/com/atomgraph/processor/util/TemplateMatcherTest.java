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
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.impl.TemplateImpl;
import com.atomgraph.processor.vocabulary.LDT;
import static junit.framework.Assert.assertEquals;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateMatcherTest
{
    
    private Ontology ontology, importedOntology, importedImportedOntology;
    private Template importedImportedTemplate, importedTemplate1, importedTemplate2, importedTemplate3, template1, template2, template3;
    private TemplateMatcher matcher;
    
    @BeforeClass
    public static void setUpClass()
    {
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);
    }
    
    @Before
    public void setUp()
    {
        ontology = ModelFactory.createOntologyModel().createOntology("http://test/ontology");
        importedOntology = ontology.getOntModel().createOntology("http://test/ontology/import");
        ontology.addImport(importedOntology);
        importedImportedOntology = ontology.getOntModel().createOntology("http://test/ontology/import/import");
        importedOntology.addImport(importedImportedOntology);

        importedImportedTemplate = importedImportedOntology.getOntModel().createClass("http://test/ontology/import/import/template").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        importedImportedTemplate.addLiteral(LDT.match, "{path}").
                addProperty(RDFS.isDefinedBy, importedImportedOntology);
        
        importedTemplate1 = importedOntology.getOntModel().createClass("http://test/ontology/import/template1").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        importedTemplate1.addLiteral(LDT.match, "{less}/{specific}/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology);
        importedTemplate2 = importedOntology.getOntModel().createClass("http://test/ontology/import/template2").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        importedTemplate2.addLiteral(LDT.match, "{other}/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology);
        importedTemplate3 = importedOntology.getOntModel().createClass("http://test/ontology/import/template3").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        importedTemplate3.addLiteral(LDT.match, "more/specific/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology);
        
        template1 = ontology.getOntModel().createClass("http://test/ontology/template1").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        template1.addLiteral(LDT.match, "more/specific/{path}").
                addProperty(RDFS.isDefinedBy, ontology);
        template2 = ontology.getOntModel().createClass("http://test/ontology/template2").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        template2.addLiteral(LDT.match, "other/{path}").
                addProperty(RDFS.isDefinedBy, ontology);
        template3 = ontology.getOntModel().createClass("http://test/ontology/template3").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        template3.addLiteral(LDT.match, "other/{path}").
                addLiteral(LDT.priority, 1). // priority takes precedence
                addProperty(RDFS.isDefinedBy, ontology);
        
        matcher = new TemplateMatcher(ontology);
    }

    /**
     * Test of match method, of class TemplateMatcher.
     */
    @Test
    public void testMatchPath()
    {
        assertEquals(importedImportedTemplate, matcher.match(ontology, "whatever"));
        assertEquals(importedTemplate1, matcher.match(ontology, "one/two/three"));
        assertEquals(importedTemplate2, matcher.match(ontology, "one/two"));
        assertEquals(template1, matcher.match(ontology, "more/specific/something"));
        assertEquals(template3, matcher.match(ontology, "other/something"));
        assertEquals(null, matcher.match(ontology, "more/specific/something/and/more"));
    }
    
    // TO-DO: move to TemplateImplTest
    @Test(expected = OntologyException.class)
    public void testTemplateWithNoPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid-ontology");
        Template invalidTemplate = invalidOntology.getOntModel().createClass("http://test/invalid-ontology/no-path-template").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        invalidTemplate.addProperty(RDFS.isDefinedBy, invalidOntology);
        
        matcher.match(invalidOntology, "other/something");
    }
    
    // TO-DO: move to TemplateImplTest
    @Test(expected = OntologyException.class)
    public void testTemplateWithNumericalPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid-ontology");
        Template invalidTemplate = invalidOntology.getOntModel().createClass("http://test/invalid-ontology/invalid-template").
                addProperty(RDF.type, LDT.Template).
                as(Template.class);
        invalidTemplate.addLiteral(LDT.match, 123).
                addProperty(RDFS.isDefinedBy, invalidOntology);
        
        matcher.match(invalidOntology, "other/something");
    }
    
}
