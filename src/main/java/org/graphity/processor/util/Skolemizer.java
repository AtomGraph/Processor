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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriTemplateParser;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.ws.rs.core.UriBuilder;
import org.graphity.processor.template.ClassTemplate;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder class that can build URIs from templates for RDF resources as well as models.
 * Needs to be initialized with sitemap ontology, ontology class matching request URI, and request URI information.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Skolemizer
{
    private static final Logger log = LoggerFactory.getLogger(Skolemizer.class);

    private OntClassMatcher ontClassMatcher;
    private UriBuilder baseUriBuilder, absolutePathBuilder;
    
    protected Skolemizer()
    {
    }
    
    protected static Skolemizer newInstance()
    {
	return new Skolemizer();
    }

    public Skolemizer baseUriBuilder(UriBuilder baseUriBuilder)
    {
	if (baseUriBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");
        this.baseUriBuilder = baseUriBuilder;
        return this;
    }

    public Skolemizer absolutePathBuilder(UriBuilder absolutePathBuilder)
    {
	if (absolutePathBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");
        this.absolutePathBuilder = absolutePathBuilder;
        return this;
    }
    
    public Skolemizer ontClassMatcher(OntClassMatcher ontClassMatcher)
    {
	if (ontClassMatcher == null) throw new IllegalArgumentException("OntClassMatcher cannot be null");
        this.ontClassMatcher = ontClassMatcher;
        return this;
    }
    
    public static Skolemizer fromOntClassMatcher(OntClassMatcher ontClassMatcher)
    {
        return newInstance().ontClassMatcher(ontClassMatcher);
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
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        
        UriBuilder builder;
        Map<String, String> nameValueMap;
        
        SortedSet<ClassTemplate> matchedTypes = getOntClassMatcher().match(resource, RDF.type, 0);
        if (!matchedTypes.isEmpty())
        {
            OntClass typeClass = matchedTypes.first().getOntClass();
            if (log.isDebugEnabled()) log.debug("Skolemizing resource {} using ontology class {}", resource, typeClass);

            // optionally, skolemization template builds with absolute path builder (e.g. "{slug}")
            String skolemTemplate = getStringValue(typeClass, GP.skolemTemplate);
            if (skolemTemplate != null)
            {
                builder = getAbsolutePathBuilder().path(skolemTemplate);
                nameValueMap = getNameValueMap(resource, new UriTemplateParser(skolemTemplate));
            }
            else // by default, URI match template builds with base URI builder (e.g. ", "{path: .*}", /files/{slug}")
            {
                String uriTemplate = getStringValue(typeClass, GP.uriTemplate);
                builder = getBaseUriBuilder().path(uriTemplate);
                nameValueMap = getNameValueMap(resource, new UriTemplateParser(uriTemplate));
            }

            // add fragment identifier for non-information resources
            if (!resource.hasProperty(RDF.type, FOAF.Document)) builder.fragment("this");
            
            if (nameValueMap != null) return builder.buildFromMap(nameValueMap);
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
    
    public OntClassMatcher getOntClassMatcher()
    {
        return ontClassMatcher;
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
