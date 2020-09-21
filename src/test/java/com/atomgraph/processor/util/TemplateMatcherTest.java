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
import com.atomgraph.server.util.OntologyLoader;
import static junit.framework.Assert.assertEquals;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class TemplateMatcherTest
{
    
    private Ontology ontology, importedOntology, importedImportedOntology;
    private Template importedImportedTemplate, importedTemplate1, importedTemplate2, template1, template3;
    private TemplateMatcher matcher;
    
    static
    {
        JenaSystem.init();
    }
    
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

        importedImportedTemplate = importedImportedOntology.getOntModel().createIndividual("http://test/ontology/import/import/template", LDT.Template).
                addLiteral(LDT.match, "{path}").
                addProperty(RDFS.isDefinedBy, importedImportedOntology).
                as(Template.class);
        
        importedTemplate1 = importedOntology.getOntModel().createIndividual("http://test/ontology/import/template1", LDT.Template).
                addLiteral(LDT.match, "{less}/{specific}/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology).
                as(Template.class);
        importedTemplate2 = importedOntology.getOntModel().createIndividual("http://test/ontology/import/template2", LDT.Template).
                addLiteral(LDT.match, "{other}/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology).
                as(Template.class);
        importedOntology.getOntModel().createIndividual("http://test/ontology/import/template3", LDT.Template).
                addLiteral(LDT.match, "more/specific/{path}").
                addProperty(RDFS.isDefinedBy, importedOntology).
                as(Template.class);
        
        template1 = ontology.getOntModel().createIndividual("http://test/ontology/template1", LDT.Template).
                addLiteral(LDT.match, "more/specific/{path}").
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        ontology.getOntModel().createIndividual("http://test/ontology/template2", LDT.Template).
                addLiteral(LDT.match, "other/{path}").
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        template3 = ontology.getOntModel().createIndividual("http://test/ontology/template3", LDT.Template).
                addLiteral(LDT.match, "other/{path}").
                addLiteral(LDT.priority, 1). // priority takes precedence
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        
        // load the ontology the same way Application loads it
        OntDocumentManager.getInstance().addModel(ontology.getURI(), ontology.getOntModel());
        ontology = new OntologyLoader( OntDocumentManager.getInstance(), ontology.getURI(), ontology.getOntModel().getSpecification(), true).getOntology();

        matcher = new TemplateMatcher(ontology);
    }

    /**
     * Test of match method, of class TemplateMatcher.
     */
    @Test
    public void testMatchPath()
    {
        assertEquals(importedImportedTemplate, matcher.match("whatever"));
        assertEquals(importedTemplate1, matcher.match("one/two/three"));
        assertEquals(importedTemplate2, matcher.match("one/two"));
        assertEquals(template1, matcher.match("more/specific/something"));
        assertEquals(template3, matcher.match("other/something"));
        assertEquals(null, matcher.match("more/specific/something/and/more"));
    }
    
    @Test(expected = OntologyException.class)
    public void testTemplateWithNoPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid-ontology");
        Template invalidTemplate = invalidOntology.getOntModel().createIndividual("http://test/invalid-ontology/no-path-template", LDT.Template).
                as(Template.class);
        invalidTemplate.addProperty(RDFS.isDefinedBy, invalidOntology);
        
        new TemplateMatcher(invalidOntology).match("other/something");
    }
    
    @Test(expected = OntologyException.class)
    public void testTemplateWithNumericalPath()
    {
        Ontology invalidOntology = ModelFactory.createOntologyModel().createOntology("http://test/invalid-ontology");
        Template invalidTemplate = invalidOntology.getOntModel().createIndividual("http://test/invalid-ontology/invalid-template", LDT.Template).
                as(Template.class);
        invalidTemplate.addLiteral(LDT.match, 123).
                addProperty(RDFS.isDefinedBy, invalidOntology);
        
        new TemplateMatcher(invalidOntology).match("other/something");
    }
    
}
