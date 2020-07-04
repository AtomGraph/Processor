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

import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.vocabulary.LDT;
import java.util.Comparator;
import java.util.Objects;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for resource template class in the sitemap ontology that matches the current request.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class TemplateMatcher
{
    private static final Logger log = LoggerFactory.getLogger(TemplateMatcher.class);

    private final Ontology ontology;
    
    public static class TemplatePrecedence
    {
        
        static public final Comparator<TemplatePrecedence> COMPARATOR = new Comparator<TemplatePrecedence>()
        {

            @Override
            public int compare(TemplatePrecedence template1, TemplatePrecedence template2)
            {
                return template2.getPrecedence() - template1.getPrecedence();
            }

        };
        
        private final Template template;
        private final int precedence;
        
        public TemplatePrecedence(Template template, int precedence)
        {
            this.template = template;
            this.precedence = precedence;
        }
        
        public Template getTemplate()
        {
            return template;
        }
        
        public int getPrecedence()
        {
            return precedence;
        }
        
        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(getPrecedence());
            hash = 59 * hash + Objects.hashCode(getTemplate().getPriority());
            hash = 59 * hash + Objects.hashCode(getTemplate().getMatch());
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final TemplatePrecedence other = (TemplatePrecedence) obj;
            return Objects.equals(getPrecedence(), other.getPrecedence());
        }
        
        @Override
        public String toString()
        {
            return new StringBuilder().
            append("[<").
            append(getTemplate().getURI()).
            append(">, ").
            append(getPrecedence()).
            append("]").
            toString();
        }
        
    }
    
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
    public Template match(URI uri, URI base)
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
    public List<TemplatePrecedence> match(Ontology ontology, CharSequence path, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (path == null) throw new IllegalArgumentException("CharSequence cannot be null");
        
        if (log.isTraceEnabled()) log.trace("Matching path '{}' against resource templates in sitemap: {}", path, ontology);
        if (log.isTraceEnabled()) log.trace("Ontology import level: {}", level);
        List<TemplatePrecedence> matches = new ArrayList<>();

        ResIterator it = ontology.getOntModel().listResourcesWithProperty(RDF.type, LDT.Template);
        try
        {
            while (it.hasNext())
            {
                Template template = it.next().as(Template.class);
                // only match templates defined in this ontology - maybe reverse loops?
                if (template.getIsDefinedBy() != null && template.getIsDefinedBy().equals(ontology))
                {
                    if (template.getMatch() == null)
                    {
                        if (log.isErrorEnabled()) log.error("Template {} does not have value for {} annotation", template, LDT.match);
                        throw new OntologyException("Template '" + template + "' does not have value for '" + LDT.match + "' annotation");
                    }

                    UriTemplate match = template.getMatch();
                    HashMap<String, String> map = new HashMap<>();

                    if (match.match(path, map))
                    {
                        if (log.isTraceEnabled()) log.trace("Path {} matched UriTemplate {}", path, match);
                        if (log.isTraceEnabled()) log.trace("Path {} matched OntClass {}", path, template);
                        
                        TemplatePrecedence precedence = new TemplatePrecedence(template, level * -1);
                        matches.add(precedence);
                    }
                    else
                        if (log.isTraceEnabled()) log.trace("Path {} did not match UriTemplate {}", path, match);
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
                matches.addAll(match(importedOntology, path, level + 1));
        }
        finally
        {
            it.close();
        }

        return matches;
    }
    
    public Template match(CharSequence path)
    {
        return match(getOntology(), path);
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
    public Template match(Ontology ontology, CharSequence path)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        
        List<TemplatePrecedence> precedences = match(ontology, path, 0);
        if (!precedences.isEmpty())
        {
            // step 1: collect matching Templates with highest import precedence
            List<Template> topMatches = new ArrayList<>();
            Collections.sort(precedences, TemplatePrecedence.COMPARATOR);
            TemplatePrecedence maxPrecedence = precedences.get(0);
            for (TemplatePrecedence precedence : precedences)
                if (precedence.equals(maxPrecedence)) topMatches.add(precedence.getTemplate());

            // step 2: Template with the highest priority is the match
            if (log.isTraceEnabled()) log.trace("{} path matched these Templates: {} (selecting the first UriTemplate)", path, precedences);
            Collections.sort(topMatches, Template.COMPARATOR);
            Template match = topMatches.get(0);
            if (log.isDebugEnabled()) log.debug("Path: {} matched Template: {}", path, match);
            
            // step3: check for conflicts (Templates with equal priority and UriTemplate)
            for (Template template : topMatches)
                if (template != match && template.equals(match))
                    if (log.isWarnEnabled()) log.warn("Path: {} has conflicting Template: {} (it is equal to the matched one)", path, template);

            return match;
        }
        
        if (log.isDebugEnabled()) log.debug("Path {} has no Template match in this OntModel", path);
        return null;
    }
    
    public Ontology getOntology()
    {
        return ontology;
    }
    
}