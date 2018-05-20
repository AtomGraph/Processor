/*
 * Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>.
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
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.api.uri.UriTemplateParser;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.ws.rs.core.UriBuilder;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.SIOC;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.OntClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder class that can build URIs from templates for RDF resources as well as models.
 * Needs to be initialized with sitemap ontology, ontology class matching request URI, and request URI information.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Skolemizer
{
    private static final Logger log = LoggerFactory.getLogger(Skolemizer.class);

    private final Ontology ontology;
    private final UriBuilder baseUriBuilder, absolutePathBuilder;
    
    public static class ClassPrecedence implements Comparable
    {

        final private OntClass ontClass;
        final private int precedence;

        public ClassPrecedence(OntClass ontClass, int precedence)
        {
            if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");

            this.ontClass = ontClass;
            this.precedence = precedence;
        }

        public final OntClass getOntClass()
        {
            return ontClass;
        }

        public final int getPrecedence()
        {
            return precedence;
        }

        @Override
        public String toString()
        {
            return new StringBuilder().
            append("[<").
            append(getOntClass().getURI()).
            append(">, ").
            append(getPrecedence()).
            append("]").
            toString();
        }

        @Override
        public int compareTo(Object obj)
        {
            ClassPrecedence template = (ClassPrecedence)obj;
            return template.getPrecedence() - getPrecedence();
        }

    }

    public Skolemizer(Ontology ontology, UriBuilder baseUriBuilder, UriBuilder absolutePathBuilder)
    {
	if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
	if (baseUriBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");
	if (absolutePathBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");
        this.ontology = ontology;        
        this.baseUriBuilder = baseUriBuilder;
        this.absolutePathBuilder = absolutePathBuilder;
    }

    public Model build(Model model)
    {
    	if (model == null) throw new IllegalArgumentException("Model cannot be null");

	Map<Resource, String> resourceURIMap = new HashMap<>();
	ResIterator resIt = model.listSubjects();
	try
	{
	    while (resIt.hasNext())
	    {
		Resource resource = resIt.next();
                if (resource.isAnon())
                {
                    URI uri = build(resource);
                    if (uri != null) resourceURIMap.put(resource, uri.toString());
                }
	    }
	}
	finally
	{
	    resIt.close();
	}
	
	Iterator<Map.Entry<Resource, String>> entryIt = resourceURIMap.entrySet().iterator();
	while (entryIt.hasNext())
	{
	    Map.Entry<Resource, String> entry = entryIt.next();
	    ResourceUtils.renameResource(entry.getKey(), entry.getValue());
	}

	return model;
    }
    
    public URI build(Resource resource)
    {
        SortedSet<ClassPrecedence> matchedClasses = match(getOntology(), resource, RDF.type, 0);
        if (!matchedClasses.isEmpty())
        {
            OntClass typeClass = matchedClasses.first().getOntClass();
            if (log.isDebugEnabled()) log.debug("Skolemizing resource {} using ontology class {}", resource, typeClass);
            
            return build(resource, typeClass);
        }
        
        return null;
    }
    
    public URI build(Resource resource, OntClass typeClass)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (typeClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        // skolemization template builds with absolute path builder (e.g. "{slug}")
        String path = getStringValue(typeClass, LDT.path);
        if (path == null)
            throw new IllegalStateException("Cannot skolemize resource of class " + typeClass + " which does not have ldt:path annotation");

        final UriBuilder builder;
        // treat paths starting with / as absolute, others as relative (to the current absolute path)
        // JAX-RS URI templates do not have this distinction (leading slash is irrelevant)
        if (path.startsWith("/"))
            builder = getBaseUriBuilder().clone();
        else
        {
            Resource parent = getParent(typeClass);
            if (parent != null) builder = UriBuilder.fromUri(parent.getURI());
            else builder = getAbsolutePathBuilder().clone();
        }

        Map<String, String> nameValueMap = getNameValueMap(resource, new UriTemplateParser(path));
        builder.path(path);

        // add fragment identifier
        String fragment = getStringValue(typeClass, LDT.fragment);
        return builder.fragment(fragment).buildFromMap(nameValueMap);
    }

    protected Map<String, String> getNameValueMap(Resource resource, UriTemplateParser parser)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (parser == null) throw new IllegalArgumentException("UriTemplateParser cannot be null");

	Map<String, String> nameValueMap = new HashMap<>();
	
        List<String> names = parser.getNames();
	for (String name : names)
	{
	    Literal literal = getLiteral(resource, name);
	    if (literal != null) nameValueMap.put(name, literal.getString());
	}

        return nameValueMap;
    }

    protected Literal getLiteral(Resource resource, String namePath)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");

	if (namePath.contains("."))
	{
	    String name = namePath.substring(0, namePath.indexOf("."));
	    String nameSubPath = namePath.substring(namePath.indexOf(".") + 1);
	    Resource subResource = getResource(resource, name);
	    if (subResource != null) return getLiteral(subResource, nameSubPath);
	}
	
	StmtIterator it = resource.listProperties();
	try
	{
	    while (it.hasNext())
	    {
		Statement stmt = it.next();
		if (stmt.getObject().isLiteral() && stmt.getPredicate().getLocalName().equals(namePath))
		{
		    if (log.isTraceEnabled()) log.trace("Found Literal {} for property name: {} ", stmt.getLiteral(), namePath);
		    return stmt.getLiteral();
		}
	    }
	}
	finally
	{
	    it.close();
	}
	
	return null;
    }

    protected Resource getResource(Resource resource, String name)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	
	StmtIterator it = resource.listProperties();
	try
	{
	    while (it.hasNext())
	    {
		Statement stmt = it.next();
		if (stmt.getObject().isAnon() && stmt.getPredicate().getLocalName().equals(name))
		{
		    if (log.isTraceEnabled()) log.trace("Found Resource {} for property name: {} ", stmt.getResource(), name);
		    return stmt.getResource();
		}
	    }
	}
	finally
	{
	    it.close();
	}
	
	return null;
    }
    
    public SortedSet<ClassPrecedence> match(Ontology ontology, Resource resource, Property property, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        SortedSet<ClassPrecedence> matchedClasses = new TreeSet<>();
        ResIterator it = ontology.getOntModel().listResourcesWithProperty(LDT.path);
        try
        {
            while (it.hasNext())
            {
                Resource ontClassRes = it.next();
                OntClass ontClass = ontology.getOntModel().getOntResource(ontClassRes).asClass();
                // only match templates defined in this ontology - maybe reverse loops?
                if (ontClass.getIsDefinedBy() != null && ontClass.getIsDefinedBy().equals(ontology) &&
                        resource.hasProperty(property, ontClass))
                {
                    ClassPrecedence template = new ClassPrecedence(ontClass, level * -1);
                    if (log.isTraceEnabled()) log.trace("Resource {} matched OntClass {}", resource, ontClass);
                    matchedClasses.add(template);
                } 
            }            
        }
        finally
        {
            it.close();
        }

        ExtendedIterator<OntResource> imports = ontology.listImports();
        try
        {
            while (imports.hasNext())
            {
                OntResource importRes = imports.next();
                if (importRes.canAs(Ontology.class))
                {
                    Ontology importedOntology = importRes.asOntology();
                    // traverse imports recursively
                    Set<ClassPrecedence> matchedImportClasses = match(importedOntology, resource, property, level + 1);
                    matchedClasses.addAll(matchedImportClasses);
                }
            }
        }
        finally
        {
            imports.close();
        }
        
        return matchedClasses;
    }

    protected String getStringValue(OntClass ontClass, Property property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        if (ontClass.hasProperty(property))
        {
            if (!ontClass.getPropertyValue(property).isLiteral() ||
                    ontClass.getPropertyValue(property).asLiteral().getDatatype() == null ||
                    !ontClass.getPropertyValue(property).asLiteral().getDatatype().equals(XSDDatatype.XSDstring))
            {
                if (log.isErrorEnabled()) log.error("Class {} property {} is not an xsd:string literal", ontClass, property);
                throw new OntologyException("Class '" + ontClass + "' property '" + property + "' is not an xsd:string literal");
            }
            
            return ontClass.getPropertyValue(property).asLiteral().getString();
        }
        
        return null;
    }
    
    public Ontology getOntology()
    {
        return ontology;
    }
    
    public UriBuilder getBaseUriBuilder()
    {
        return baseUriBuilder;
    }

    // TO-DO: move to a LDTDH (document hierarchy) specific Skolemizer subclass
    public Resource getParent(OntClass ontClass)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        ExtendedIterator<OntClass> hasValueIt = ontClass.listSuperClasses();
        try
        {
            while (hasValueIt.hasNext())
            {
                OntClass superClass = hasValueIt.next();
                
                if (superClass.canAs(HasValueRestriction.class))
                {
                    HasValueRestriction hvr = superClass.as(HasValueRestriction.class);
                    if (hvr.getOnProperty().equals(SIOC.HAS_PARENT) || hvr.getOnProperty().equals(SIOC.HAS_CONTAINER))
                    {
                        if (!hvr.getHasValue().isURIResource())
                        {
                            if (log.isErrorEnabled()) log.error("HasValue restriction on class {} for property {} is not a URI resource", ontClass, hvr.getOnProperty());
                            throw new OntologyException("HasValue restriction on class '" + ontClass + "' for property '" + hvr.getOnProperty() + "' is not a URI resource");
                        }
                        
                        return hvr.getHasValue().asResource();
                    }
                }
            }
        }
        finally
        {
            hasValueIt.close();
        }
        
        return null;
    }
        
    public UriBuilder getAbsolutePathBuilder()
    {
        return absolutePathBuilder;
    }
    
}
