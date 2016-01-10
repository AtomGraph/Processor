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

import org.graphity.processor.template.ClassTemplate;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.template.UriClassTemplate;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for resource template class in the sitemap ontology that matches the current request.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class OntClassMatcher
{
    private static final Logger log = LoggerFactory.getLogger(OntClassMatcher.class);

    private final Ontology ontology;
    
    public OntClassMatcher(Ontology ontology)
    {
        this.ontology = ontology;
    }
    
    /**
     * Given an absolute URI and a base URI, returns ontology class with a matching URI template, if any.
     * 
     * @param uri absolute URI being matched
     * @param base base URI
     * @return matching ontology class or null, if none
     */
    public OntClass match(URI uri, URI base)
    {
	if (uri == null) throw new IllegalArgumentException("URI being matched cannot be null");
	if (base == null) throw new IllegalArgumentException("Base URI cannot be null");
	if (!uri.isAbsolute()) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not absolute");
	if (base.relativize(uri).equals(uri)) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not relative to the base URI \"" + base + "\"");
	    
	StringBuilder path = new StringBuilder();
	// instead of path, include query string by relativizing request URI against base URI
	path.append("/").append(base.relativize(uri));
	ClassTemplate template = match(getOntology(), path);
        if (template != null) return template.getOntClass();
        
        return null;
    }
            
    /**
     * Matches path (relative URI) against URI templates in sitemap ontology.
     * This method uses Jersey implementation of the JAX-RS URI matching algorithm.
     * 
     * @param ontology sitemap ontology
     * @param path URI path
     * @param level
     * @return URI template/class mapping
     */    
    public List<UriClassTemplate> match(Ontology ontology, CharSequence path, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (path == null) throw new IllegalArgumentException("CharSequence cannot be null");
        
        if (log.isTraceEnabled()) log.trace("Matching path '{}' against resource templates in sitemap: {}", path, ontology);
        if (log.isTraceEnabled()) log.trace("Ontology import level: {}", level);
        List<UriClassTemplate> matchedClasses = new ArrayList<>();

        ResIterator it = ontology.getOntModel().listResourcesWithProperty(RDF.type, GP.Template);
        try
        {
            while (it.hasNext())
            {
                Resource templateRes = it.next();
                OntClass ontClass = ontology.getOntModel().getOntResource(templateRes).asClass();
                // only match templates defined in this ontology - maybe reverse loops?
                if (ontClass.getIsDefinedBy() != null && ontClass.getIsDefinedBy().equals(ontology))
                {
                    if (!templateRes.hasProperty(GP.uriTemplate))
                    {
                        if (log.isDebugEnabled()) log.debug("Template class {} does not have value for {} annotation", templateRes, GP.uriTemplate);
                        throw new SitemapException("Template class '" + templateRes + "' does not have value for '" + GP.uriTemplate + "' annotation");
                    }

                    UriTemplate uriTemplate = new UriTemplate(templateRes.getProperty(GP.uriTemplate).getString());
                    HashMap<String, String> map = new HashMap<>();

                    if (uriTemplate.match(path, map))
                    {
                        UriClassTemplate template = new UriClassTemplate(ontClass, new Double(level * -1), uriTemplate);
                        if (log.isTraceEnabled()) log.trace("Path {} matched UriTemplate {}", path, uriTemplate);
                        if (log.isTraceEnabled()) log.trace("Path {} matched OntClass {}", path, ontClass);
                        matchedClasses.add(template);
                    }
                    else
                        if (log.isTraceEnabled()) log.trace("Path {} did not match UriTemplate {}", path, uriTemplate);
                }
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
                        matchedClasses.addAll(match(importedOntology, path, level + 1));
                    }
                }
            }
            finally
            {
                imports.close();
            }
        }
        finally
        {
            it.close();
        }

        return matchedClasses;
    }
    
    /**
     * Given a relative URI and URI template property, returns ontology class with a matching URI template, if any.
     * URIs are matched against the URI templates specified in resource templates (sitemap ontology classes).
     * Templates in the base ontology model have priority (are matched first) against templates in imported ontologies.
     * 
     * @param ontology sitemap ontology model
     * @param path absolute path (relative URI)
     * @return matching ontology class or null, if none
d     * @see <a href="https://jsr311.java.net/nonav/releases/1.1/spec/spec3.html#x3-340003.7">3.7 Matching Requests to Resource Methods (JAX-RS 1.1)</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/uri/UriTemplate.html">Jersey UriTemplate</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/ontology/HasValueRestriction.html">Jena HasValueRestriction</a>
     */
    public UriClassTemplate match(Ontology ontology, CharSequence path)
    {
	if (ontology == null) throw new IllegalArgumentException("OntModel cannot be null");
        
        List<UriClassTemplate> matchedTemplates = match(ontology, path, 0);
        if (!matchedTemplates.isEmpty())
        {
            if (log.isDebugEnabled()) log.debug("{} path matched these Templates: {} (selecting the first UriTemplate)", path, matchedTemplates);
            Collections.sort(matchedTemplates, UriClassTemplate.COMPARATOR);

            UriClassTemplate match = matchedTemplates.get(0);            
            if (log.isDebugEnabled()) log.debug("Path: {} matched Template: {}", path, match);
            
            // Check for conflicts: Templates with identical UriTemplate and precedence
            for (UriClassTemplate template : matchedTemplates)
                if (!template.getOntClass().equals(match.getOntClass()) && template.equals(match))
                    if (log.isErrorEnabled()) log.error("Path: {} has conflicting Template: {} (it is equal to the matched one)", path, template);

            return match;
        }
        
        if (log.isDebugEnabled()) log.debug("Path {} has no Template match in this OntModel", path);
        return null;
    }

    public SortedSet<ClassTemplate> match(Ontology ontology, Resource resource, Property property, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        SortedSet<ClassTemplate> matchedClasses = new TreeSet<>();
        ResIterator it = ontology.getOntModel().listResourcesWithProperty(RDF.type, OWL.Class); // some classes are not templates!
        try
        {
            while (it.hasNext())
            {
                Resource ontClassRes = it.next();
                OntClass ontClass = ontology.getOntModel().getOntResource(ontClassRes).asClass();
                // only match templates defined in this ontology - maybe reverse loops?
                if (ontClass.getIsDefinedBy() != null && ontClass.getIsDefinedBy().equals(ontology))
                {
                   if (ontClass.hasProperty(GP.skolemTemplate) && resource.hasProperty(property, ontClass))
                    {
                        ClassTemplate template = new ClassTemplate(ontClass, new Double(level * -1));
                        if (log.isDebugEnabled()) log.debug("Resource {} matched OntClass {}", resource, ontClass);
                        matchedClasses.add(template);
                    }
 
                }
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
        }
        finally
        {
            it.close();
        }
            
        return matchedClasses;
    }
    
    public OntModel getOntModel()
    {
        return getOntology().getOntModel();
    }

    public Ontology getOntology()
    {
        return ontology;
    }
    
}