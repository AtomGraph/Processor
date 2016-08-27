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

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.model.Template;
import org.graphity.processor.model.TemplateCall;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for resource template class in the sitemap ontology that matches the current request.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TemplateMatcher
{
    private static final Logger log = LoggerFactory.getLogger(TemplateMatcher.class);

    private final Ontology ontology;
    
    public TemplateMatcher(Ontology ontology)
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
    public TemplateCall match(URI uri, URI base)
    {
	if (uri == null) throw new IllegalArgumentException("URI being matched cannot be null");
	if (base == null) throw new IllegalArgumentException("Base URI cannot be null");
	if (!uri.isAbsolute()) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not absolute");
	if (base.relativize(uri).equals(uri)) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not relative to the base URI \"" + base + "\"");
	    
	StringBuilder path = new StringBuilder();
	// instead of path, include query string by relativizing request URI against base URI
	path.append("/").append(base.relativize(uri));
	return match(getOntology(), path);
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
    public List<TemplateCall> match(Ontology ontology, CharSequence path, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (path == null) throw new IllegalArgumentException("CharSequence cannot be null");
        
        if (log.isTraceEnabled()) log.trace("Matching path '{}' against resource templates in sitemap: {}", path, ontology);
        if (log.isTraceEnabled()) log.trace("Ontology import level: {}", level);
        List<TemplateCall> templateCalls = new ArrayList<>();

        ResIterator it = ontology.getOntModel().listResourcesWithProperty(RDF.type, GP.Template);
        try
        {
            while (it.hasNext())
            {
                //Resource templateRes = it.next();
                Template template = it.next().as(Template.class);
                // only match templates defined in this ontology - maybe reverse loops?
                if (template.getIsDefinedBy() != null && template.getIsDefinedBy().equals(ontology))
                {
                    if (template.getPath() == null)
                    {
                        if (log.isDebugEnabled()) log.debug("Template class {} does not have value for {} annotation", template, GP.path);
                        throw new SitemapException("Template class '" + template + "' does not have value for '" + GP.path + "' annotation");
                    }

                    UriTemplate uriTemplate = template.getPath();
                    HashMap<String, String> map = new HashMap<>();

                    if (uriTemplate.match(path, map))
                    {
                        TemplateCall templateCall = ontology.getOntModel().createIndividual(GP.TemplateCall).
                            addProperty(GP.template, template).
                            addLiteral(GP.priority, new Double(level * -1)). // precedence instead of priority?
                            as(TemplateCall.class);

                        if (log.isTraceEnabled()) log.trace("Path {} matched UriTemplate {}", path, uriTemplate);
                        if (log.isTraceEnabled()) log.trace("Path {} matched OntClass {}", path, template);
                        templateCalls.add(templateCall);
                    }
                    else
                        if (log.isTraceEnabled()) log.trace("Path {} did not match UriTemplate {}", path, uriTemplate);
                }
            }

            List<Ontology> importedOntologies = new ArrayList<>(); // collect imports first to avoid CME within iterator
            ExtendedIterator<OntResource> importIt = ontology.listImports();
            try
            {
                while (importIt.hasNext())
                {
                    OntResource importRes = importIt.next();
                    if (importRes.canAs(Ontology.class)) importedOntologies.add(importRes.asOntology());
                }
            }
            finally
            {
                importIt.close();
            }
            
            //traverse imports recursively, safely make changes to OntModel outside the iterator
            for (Ontology importedOntology : importedOntologies)
                templateCalls.addAll(match(importedOntology, path, level + 1));            
        }
        finally
        {
            it.close();
        }

        return templateCalls;
    }
    
    /**
     * Given a relative URI and URI template property, returns ontology class with a matching URI template, if any.
     * URIs are matched against the URI templates specified in resource templates (sitemap ontology classes).
     * Templates in the base ontology model have priority (are matched first) against templates in imported ontologies.
     * 
     * @param ontology sitemap ontology model
     * @param path absolute path (relative URI)
     * @return matching ontology class or null, if none
     * @see <a href="https://jsr311.java.net/nonav/releases/1.1/spec/spec3.html#x3-340003.7">3.7 Matching Requests to Resource Methods (JAX-RS 1.1)</a>
     * @see <a href="https://jersey.java.net/nonav/apidocs/1.16/jersey/com/sun/jersey/api/uri/UriTemplate.html">Jersey UriTemplate</a>
     */
    public TemplateCall match(Ontology ontology, CharSequence path)
    {
	if (ontology == null) throw new IllegalArgumentException("OntModel cannot be null");
        
        List<TemplateCall> templateCalls = match(ontology, path, 0);
        if (!templateCalls.isEmpty())
        {
            if (log.isTraceEnabled()) log.trace("{} path matched these Templates: {} (selecting the first UriTemplate)", path, templateCalls);
            Collections.sort(templateCalls, TemplateCall.COMPARATOR);

            TemplateCall match = templateCalls.get(0);            
            if (log.isDebugEnabled()) log.debug("Path: {} matched Template: {}", path, match);
            
            // Check for conflicts: Templates with identical UriTemplate and precedence
            for (TemplateCall templateCall : templateCalls)
                if (!templateCall.getTemplate().equals(match.getTemplate()) && templateCall.equals(match))
                    if (log.isErrorEnabled()) log.error("Path: {} has conflicting Template: {} (it is equal to the matched one)", path, templateCall);

            return match;
        }
        
        if (log.isDebugEnabled()) log.debug("Path {} has no Template match in this OntModel", path);
        return null;
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