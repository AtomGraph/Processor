/*
 * Copyright 2014 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.util;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.uri.UriComponent;
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
import org.graphity.processor.template.ClassTemplate;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.SIOC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.util.JenaUtil;

/**
 * Builder class that can build URIs from templates for RDF resources as well as models.
 * Needs to be initialized with sitemap ontology, ontology class matching request URI, and request URI information.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Skolemizer
{
    private static final Logger log = LoggerFactory.getLogger(Skolemizer.class);

    private final Ontology ontology;
    private final UriBuilder baseUriBuilder, absolutePathBuilder;
    
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
    
    public URI build(Resource resource) throws IllegalArgumentException
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        
        UriBuilder builder;
        Map<String, String> nameValueMap;
        
        SortedSet<ClassTemplate> matchedTypes = match(getOntology(), resource, RDF.type, 0);
        if (!matchedTypes.isEmpty())
        {
            OntClass typeClass = matchedTypes.first().getOntClass();
            if (log.isDebugEnabled()) log.debug("Skolemizing resource {} using ontology class {}", resource, typeClass);

            // optionally, skolemization template builds with absolute path builder (e.g. "{slug}")
            String skolemTemplate = getStringValue(typeClass, GP.skolemTemplate);
            if (skolemTemplate != null)
            {
                builder = getAbsolutePathBuilder().clone();
                nameValueMap = getNameValueMap(resource, new UriTemplateParser(skolemTemplate));
                // container specified in resource description can override the default one (absolute path)
                if (resource.hasProperty(SIOC.HAS_PARENT)) builder = UriBuilder.fromUri(resource.getPropertyResourceValue(SIOC.HAS_PARENT).getURI());
                if (resource.hasProperty(SIOC.HAS_CONTAINER)) builder = UriBuilder.fromUri(resource.getPropertyResourceValue(SIOC.HAS_CONTAINER).getURI());
                builder.path(skolemTemplate);
            }
            else // by default, URI match template builds with base URI builder (e.g. ", "{path: .*}", /files/{slug}")
            {
                String uriTemplate = getStringValue(typeClass, GP.uriTemplate);
                builder = getBaseUriBuilder().clone().path(uriTemplate);
                nameValueMap = getNameValueMap(resource, new UriTemplateParser(uriTemplate));
            }

            // add fragment identifier for non-information resources
            boolean isInfoRes = true;
            StmtIterator it = resource.listProperties(RDF.type);
            while (it.hasNext())
            {
                Statement stmt = it.next();
                Resource type = getOntology().getOntModel().getResource(stmt.getResource().getURI());
                if (JenaUtil.hasSuperClass(type, GP.Document)) isInfoRes = false;
                //if (hasSuperClass(type, FOAF.Document));
            }
            if (isInfoRes) builder.fragment("this"); // TO-DO: unique identifiers per resource?
            
            return builder.buildFromMap(nameValueMap); // if (!nameValueMap.isEmpty())
        }
        
        return null;
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
	    if (literal != null)
	    {
                // %-encode the parameter value as UriBuilder.buildFromMap() in Jersey 1.x fails to do that
                // https://java.net/jira/browse/JAX_RS_SPEC-70
                String value = UriComponent.contextualEncode(literal.getString(), UriComponent.Type.PATH_SEGMENT);
                if (log.isDebugEnabled()) log.debug("UriTemplate variable name: {} has value: {}", name, value);
		nameValueMap.put(name, value);
	    }
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

    protected String getStringValue(OntClass ontClass, Property property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getString();
        
        return null;
    }
    public SortedSet<ClassTemplate> match(Ontology ontology, Resource resource, Property property, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        SortedSet<ClassTemplate> matchedClasses = new TreeSet<>();
        ResIterator it = ontology.getOntModel().listResourcesWithProperty(GP.skolemTemplate);
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
                    ClassTemplate template = new ClassTemplate(ontClass, new Double(level * -1));
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
                    Set<ClassTemplate> matchedImportClasses = match(importedOntology, resource, property, level + 1);
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
    
    public Ontology getOntology()
    {
        return ontology;
    }

    public UriBuilder getBaseUriBuilder()
    {
        return baseUriBuilder;
    }

    public UriBuilder getAbsolutePathBuilder()
    {
        return absolutePathBuilder;
    }
    
}
