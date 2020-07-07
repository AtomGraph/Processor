/*
 * Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.impl.ParameterImpl;
import com.atomgraph.processor.model.impl.TemplateImpl;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.spinrdf.vocabulary.SP;
import com.atomgraph.spinrdf.vocabulary.SPL;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateTest
{

    static
    {
        JenaSystem.init();
    }
    
//    private final String rulesString = "[inhClass: (?class rdf:type <http://www.w3.org/2000/01/rdf-schema#Class>), (?class ?p ?o), (?p rdf:type <https://www.w3.org/ns/ldt#InheritedProperty>), (?subClass rdfs:subClassOf ?class), (?subClass rdf:type <http://www.w3.org/2000/01/rdf-schema#Class>), noValue(?subClass ?p) -> (?subClass ?p ?o) ]\n" +
//"[inhTemplate: (?template rdf:type <https://www.w3.org/ns/ldt#Template>), (?template ?p ?o), (?p rdf:type <https://www.w3.org/ns/ldt#InheritedProperty>), (?subTemplate <https://www.w3.org/ns/ldt#extends> ?template), (?subTemplate rdf:type <https://www.w3.org/ns/ldt#Template>), noValue(?subTemplate ?p) -> (?subTemplate ?p ?o) ]\n" +
//"[rdfs9: (?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y)]";
    private Ontology ontology;
    private Template subTemplate, superTemplate, template;

    @BeforeClass
    public static void setUpClass()
    {
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);
        BuiltinPersonalities.model.add(Parameter.class, ParameterImpl.factory);
    }
    
    @Before
    public void setUp()
    {
        // old reasoner setup with custom inheritance rules
//        List<Rule> rules = Rule.parseRules(rulesString);
//        OntModelSpec rulesSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
//        Reasoner reasoner = new GenericRuleReasoner(rules);
//        rulesSpec.setReasoner(reasoner);
//        ontology = ModelFactory.createOntologyModel(rulesSpec).createOntology("http://test/ontology");
       
        ontology = ModelFactory.createOntologyModel().createOntology("http://test/ontology"); //ModelFactory.createOntologyModel(rulesSpec).createOntology("http://test/ontology");
        ontology.addImport(LDT.NAMESPACE);
        ontology.getOntModel().loadImports();
        
        Parameter param = ontology.getOntModel().createIndividual("http://test/ontology/param", LDT.Parameter).
                addProperty(SPL.predicate, FOAF.name).
                as(Parameter.class);
        superTemplate = ontology.getOntModel().createIndividual("http://test/ontology/super-template", LDT.Template).
                addProperty(LDT.param, param).
                addLiteral(LDT.priority, 5).
                addLiteral(LDT.match, "{path}").
                addLiteral(LDT.fragment, "{fragment}").
                addProperty(LDT.query, ontology.getOntModel().createIndividual("http://test/query", SP.Describe).
                        addLiteral(SP.text, "DESCRIBE ?this { ?this ?p ?o }")).
                addProperty(LDT.update, ontology.getOntModel().createIndividual("http://test/update", SP.DeleteWhere).
                        addLiteral(SP.text, "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }")).
                addProperty(LDT.lang, ontology.getOntModel().createList().
                        with(ontology.getOntModel().createLiteral("en")).
                        with(ontology.getOntModel().createLiteral("da"))).
                addLiteral(LDT.cacheControl, "max-age=3600").
                addProperty(LDT.loadClass, ontology.getOntModel().createResource("java:some.Class")).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        subTemplate = ontology.getOntModel().createIndividual("http://test/ontology/sub-template", LDT.Template).
                addProperty(LDT.extends_, superTemplate).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        template = ontology.getOntModel().createIndividual("http://test/ontology/template", LDT.Template).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
    }

    @Test
    public void testNonNullSuperTemplates()
    {
        assertNotNull(template.getSuperTemplates());
    }

    @Test
    public void testNonNullParams()
    {
        assertNotNull(template.getParameters());
    }

    @Test
    public void testNonNullLanguages()
    {
        assertNotNull(template.getLanguages());
    }
    
    @Test
    public void testInheritedMatch()
    {
        assertNotNull(superTemplate.getMatch());
        assertEquals(subTemplate.getMatch(), superTemplate.getMatch());
    }

    @Test
    public void testInheritedQuery()
    {
        assertNotNull(superTemplate.getQuery());
        assertEquals(subTemplate.getQuery(), superTemplate.getQuery());
    }

    @Test
    public void testInheritedUpdate()
    {
        assertNotNull(superTemplate.getUpdate());
        assertEquals(subTemplate.getUpdate(), superTemplate.getUpdate());
    }

    @Test
    public void testInheritedPriority()
    {
        assertNotNull(superTemplate.getPriority());
        assertEquals(subTemplate.getPriority(), superTemplate.getPriority());
    }

    @Test
    public void testInheritedParams()
    {
        assertNotNull(superTemplate.getParameters());
        assertEquals(subTemplate.getParameters(), superTemplate.getParameters());
    }
    
    @Test
    public void testInheritedCacheControl()
    {
        assertNotNull(superTemplate.getCacheControl());
        assertEquals(subTemplate.getCacheControl(), superTemplate.getCacheControl());
    }
    
    @Test
    public void testInheritedLoadClass()
    {
        assertNotNull(superTemplate.getLoadClass());
        assertEquals(subTemplate.getLoadClass(), superTemplate.getLoadClass());
    }
    
    @Test
    public void testInheritedFragment()
    {
        assertNotNull(superTemplate.getFragmentTemplate());
        assertEquals(subTemplate.getFragmentTemplate(), superTemplate.getFragmentTemplate());
    }

}
