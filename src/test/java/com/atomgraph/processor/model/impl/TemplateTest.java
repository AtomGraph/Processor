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
package com.atomgraph.processor.model.impl;

import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.spinrdf.vocabulary.SP;
import com.atomgraph.spinrdf.vocabulary.SPL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.CacheControl;
import static junit.framework.Assert.assertEquals;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDFS;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
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
    private Template superSuperTemplate, superTemplate, superTemplateOverriding, subTemplate, subTemplate1, template;

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
        
        // build 2 chains of templates that extend each other and inherit properties from the top one
        // one chain inherits all properties from the super-super-template, the other one overrides them at the super-super level
        superSuperTemplate = ontology.getOntModel().createIndividual("http://test/ontology/super-super-template", LDT.Template).
                addLiteral(LDT.priority, 5).
                addLiteral(LDT.match, "{path}").
                addLiteral(LDT.fragment, "{fragment}").
                addProperty(LDT.query, ontology.getOntModel().createIndividual("http://test/query", SP.Describe)).
                addProperty(LDT.update, ontology.getOntModel().createIndividual("http://test/update", SP.DeleteWhere)).
                addProperty(LDT.param, ontology.getOntModel().createIndividual("http://test/ontology/param", LDT.Parameter).
                    addProperty(SPL.predicate, FOAF.name)).
                addProperty(LDT.lang, ontology.getOntModel().createList().
                        with(ontology.getOntModel().createLiteral("en")).
                        with(ontology.getOntModel().createLiteral("da"))).
                addLiteral(LDT.cacheControl, "max-age=3600").
                addProperty(LDT.loadClass, ontology.getOntModel().createResource("java:some.Class")).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        superTemplate = ontology.getOntModel().createIndividual("http://test/ontology/super-template", LDT.Template).
                addProperty(LDT.extends_, superSuperTemplate).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        superTemplateOverriding = ontology.getOntModel().createIndividual("http://test/ontology/super-template-overriding", LDT.Template).
                addProperty(LDT.extends_, superSuperTemplate).
                addLiteral(LDT.priority, 7).
                addLiteral(LDT.match, "/whatever").
                addLiteral(LDT.fragment, "est").
                addProperty(LDT.query, ontology.getOntModel().createIndividual("http://test/query-overriding", SP.Describe)).
                addProperty(LDT.update, ontology.getOntModel().createIndividual("http://test/update-overriding", SP.DeleteWhere)).
                addProperty(LDT.param, ontology.getOntModel().createIndividual("http://test/ontology/param-overriding", LDT.Parameter).
                    addProperty(SPL.predicate, FOAF.maker)).
                addProperty(LDT.lang, ontology.getOntModel().createList().
                        with(ontology.getOntModel().createLiteral("lt")).
                        with(ontology.getOntModel().createLiteral("da"))).
                addLiteral(LDT.cacheControl, "max-age=9999").
                addProperty(LDT.loadClass, ontology.getOntModel().createResource("java:some.OtherClass")).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        subTemplate = ontology.getOntModel().createIndividual("http://test/ontology/sub-template", LDT.Template).
                addProperty(LDT.extends_, superTemplate).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        subTemplate1 = ontology.getOntModel().createIndividual("http://test/ontology/sub-template1", LDT.Template).
                addProperty(LDT.extends_, superTemplateOverriding).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
        template = ontology.getOntModel().createIndividual("http://test/ontology/template", LDT.Template).
                addProperty(RDFS.isDefinedBy, ontology).
                as(Template.class);
    }

    @Test
    public void testSuperTemplates()
    {
        List<Template> superTemplates = Arrays.asList(superTemplate, superSuperTemplate);
        assertEquals(superTemplates, subTemplate.getSuperTemplates());
        
        List<Template> superTemplates1 = Arrays.asList(superTemplateOverriding, superSuperTemplate);
        assertEquals(superTemplates1, subTemplate1.getSuperTemplates());
    }

    @Test
    public void testNoSuperTemplates()
    {
        assertEquals(Collections.emptyList(), template.getSuperTemplates());
    }
    
    @Test
    public void testNonNullLanguages()
    {
        List<Locale> superSuperLanguages = superSuperTemplate.getProperty(LDT.lang).getList().
                asJavaList().stream().map(n -> Locale.forLanguageTag(n.asLiteral().getString())).collect(Collectors.toList());
        assertEquals(superSuperLanguages, superSuperTemplate.getLanguages());
        assertEquals(superSuperLanguages, superTemplate.getLanguages());
        assertEquals(superSuperLanguages, subTemplate.getLanguages());
        
        List<Locale> superLanguages = superTemplateOverriding.getProperty(LDT.lang).getList().
                asJavaList().stream().map(n -> Locale.forLanguageTag(n.asLiteral().getString())).collect(Collectors.toList());
        assertEquals(superLanguages, superTemplateOverriding.getLanguages());
        assertEquals(superLanguages, subTemplate1.getLanguages());
    }
    
    @Test
    public void testInheritedMatch()
    {
        UriTemplate superSuperMatch = new UriTemplate(superSuperTemplate.getProperty(LDT.match).getString());
        assertEquals(superSuperMatch, superSuperTemplate.getMatch());
        assertEquals(superSuperMatch, superTemplate.getMatch());
        assertEquals(superSuperMatch, subTemplate.getMatch());
        
        UriTemplate superMatch = new UriTemplate(superTemplateOverriding.getProperty(LDT.match).getString());
        assertEquals(superMatch, superTemplateOverriding.getMatch());
        assertEquals(superMatch, subTemplate1.getMatch());
    }

    @Test(expected = OntologyException.class)
    public void testMissingQuery()
    {
        template.getQuery();
    }
    
    @Test
    public void testInheritedQuery()
    {
        Resource superSuperQuery = superSuperTemplate.getProperty(LDT.query).getResource();
        assertEquals(superSuperQuery, superSuperTemplate.getQuery());
        assertEquals(superSuperQuery, superTemplate.getQuery());
        assertEquals(superSuperQuery, subTemplate.getQuery());
        
        Resource superQuery = superTemplateOverriding.getProperty(LDT.query).getResource();
        assertEquals(superQuery, superTemplateOverriding.getQuery());
        assertEquals(superQuery, subTemplate1.getQuery());
    }

    @Test
    public void testInheritedUpdate()
    {
        Resource superSuperUpdate = superSuperTemplate.getProperty(LDT.update).getResource();
        assertEquals(superSuperUpdate, superSuperTemplate.getUpdate());
        assertEquals(superSuperUpdate, superTemplate.getUpdate());
        assertEquals(superSuperUpdate, subTemplate.getUpdate());
        
        Resource superUpdate = superTemplateOverriding.getProperty(LDT.update).getResource();
        assertEquals(superUpdate, superTemplateOverriding.getUpdate());
        assertEquals(superUpdate, subTemplate1.getUpdate());
    }

    @Test
    public void testInheritedPriority()
    {
        Double superSuperPriority = superSuperTemplate.getProperty(LDT.priority).getDouble();
        assertEquals(superSuperPriority, superSuperTemplate.getPriority());
        assertEquals(superSuperPriority, superTemplate.getPriority());
        assertEquals(superSuperPriority, subTemplate.getPriority());
        
        Double superPriority = superTemplateOverriding.getProperty(LDT.priority).getDouble();
        assertEquals(superPriority, superTemplateOverriding.getPriority());
        assertEquals(superPriority, subTemplate1.getPriority());
    }

    @Test
    public void testInheritedParams()
    {
        Resource superSuperParam = superSuperTemplate.getPropertyResourceValue(LDT.param);
        Set<Parameter> superSuperParams = new HashSet<>(superSuperTemplate.getParameters().values());
        assertEquals(Collections.singleton(superSuperParam), superSuperParams);
        Set<Parameter> superParams = new HashSet<>(superTemplate.getParameters().values());
        assertEquals(Collections.singleton(superSuperParam), superParams);
        Set<Parameter> subParams = new HashSet<>(subTemplate.getParameters().values());
        assertEquals(Collections.singleton(superSuperParam), subParams);
        
        Set<Resource> superParamsOverriding = new HashSet<>();
        superParamsOverriding.add(superSuperParam);
        superParamsOverriding.add(superTemplateOverriding.getPropertyResourceValue(LDT.param));
        
        Set<Parameter> superParams1 = new HashSet<>(superTemplateOverriding.getParameters().values());
        assertEquals(superParamsOverriding, superParams1);
        Set<Parameter> subParams1 = new HashSet<>(subTemplate1.getParameters().values());
        assertEquals(superParamsOverriding, subParams1);
    }
    
    @Test
    public void testInheritedCacheControl()
    {
        CacheControl superSuperCacheControl = CacheControl.valueOf(superSuperTemplate.getProperty(LDT.cacheControl).getString());
        assertEquals(superSuperCacheControl, superSuperTemplate.getCacheControl());
        assertEquals(superSuperCacheControl, superTemplate.getCacheControl());
        assertEquals(superSuperCacheControl, subTemplate.getCacheControl());
        
        CacheControl superCacheControl = CacheControl.valueOf(superTemplateOverriding.getProperty(LDT.cacheControl).getString());
        assertEquals(superCacheControl, superTemplateOverriding.getCacheControl());
        assertEquals(superCacheControl, subTemplate1.getCacheControl());
    }
    
    @Test
    public void testInheritedLoadClass()
    {
        Resource superSuperLoadClas = superSuperTemplate.getProperty(LDT.loadClass).getResource();
        assertEquals(superSuperLoadClas, superSuperTemplate.getLoadClass());
        assertEquals(superSuperLoadClas, superTemplate.getLoadClass());
        assertEquals(superSuperLoadClas, subTemplate.getLoadClass());
        
        Resource superLoadClass = superTemplateOverriding.getProperty(LDT.loadClass).getResource();
        assertEquals(superLoadClass, superTemplateOverriding.getLoadClass());
        assertEquals(superLoadClass, subTemplate1.getLoadClass());
    }
    
    @Test
    public void testInheritedFragmentTemplate()
    {
        String superSuperFragment = superSuperTemplate.getProperty(LDT.fragment).getString();
        assertEquals(superSuperFragment, superSuperTemplate.getFragmentTemplate());
        assertEquals(superSuperFragment, superTemplate.getFragmentTemplate());
        assertEquals(superSuperFragment, subTemplate.getFragmentTemplate());
        
        String superFragment = superTemplateOverriding.getProperty(LDT.fragment).getString();
        assertEquals(superFragment, superTemplateOverriding.getFragmentTemplate());
        assertEquals(superFragment, subTemplate1.getFragmentTemplate());
    }

}
