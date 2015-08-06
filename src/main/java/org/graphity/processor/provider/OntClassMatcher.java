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

package org.graphity.processor.provider;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import org.graphity.core.exception.ConfigurationException;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for resource template class in the sitemap ontology that matches the current request.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class OntClassMatcher extends PerRequestTypeInjectableProvider<Context, OntClass> implements ContextResolver<OntClass>
{
    private static final Logger log = LoggerFactory.getLogger(OntClassMatcher.class);

    @Context UriInfo uriInfo;
    @Context Providers providers;
    @Context ServletConfig servletConfig;
    
    public OntClassMatcher()
    {
	super(OntClass.class);
    }

    @Override
    public Injectable<OntClass> getInjectable(ComponentContext cc, Context a)
    {
	return new Injectable<OntClass>()
	{
	    @Override
	    public OntClass getValue()
	    {
                return getOntClass();
	    }
	};
    }

    @Override
    public OntClass getContext(Class<?> type)
    {
        return getOntClass();
    }

    public OntClass getOntClass()
    {
        return matchOntClass(getServletConfig(), getOntology(), getUriInfo().getAbsolutePath(), getUriInfo().getBaseUri());
    }
    
    /**
     * Given an absolute URI and a base URI, returns ontology class with a matching URI template, if any.
     * 
     * @param servletConfig
     * @param ontology sitemap ontology model
     * @param uri absolute URI being matched
     * @param base base URI
     * @return matching ontology class or null, if none
     */
    public OntClass matchOntClass(ServletConfig servletConfig, Ontology ontology, URI uri, URI base) // throws ConfigurationException
    {
	if (uri == null) throw new IllegalArgumentException("URI being matched cannot be null");
	if (base == null) throw new IllegalArgumentException("Base URI cannot be null");
	if (!uri.isAbsolute()) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not absolute");
	if (base.relativize(uri).equals(uri)) throw new IllegalArgumentException("URI being matched \"" + uri + "\" is not relative to the base URI \"" + base + "\"");
	    
	StringBuilder path = new StringBuilder();
	// instead of path, include query string by relativizing request URI against base URI
	path.append("/").append(base.relativize(uri));
	return matchTemplate(ontology, path).getOntClass();
    }
    
    public Query getQuery(Query query, QuerySolutionMap qsm)
    {
        if (query == null) throw new IllegalArgumentException("Query cannot be null");
        if (qsm == null) throw new IllegalArgumentException("QuerySolution cannot be null");
        
        return new ParameterizedSparqlString(query.toString(), qsm).asQuery();
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
    public Map<UriTemplate, List<Template>> matchTemplates(Ontology ontology, CharSequence path, int level)
    {
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (path == null) throw new IllegalArgumentException("CharSequence cannot be null");
        
        if (log.isDebugEnabled()) log.debug("Matching path '{}' against resource templates in sitemap: {}", path, ontology);
        if (log.isDebugEnabled()) log.debug("Ontology import level: {}", level);
        Map<UriTemplate, List<Template>> matchedClasses = new HashMap<>();

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
                        throw new ConfigurationException("Template class '" + templateRes + "' does not have value for '" + GP.uriTemplate + "' annotation");
                    }

                    UriTemplate uriTemplate = new UriTemplate(templateRes.getProperty(GP.uriTemplate).getString());
                    HashMap<String, String> map = new HashMap<>();

                    if (uriTemplate.match(path, map))
                    {
                        Template template = new Template(ontClass, new Double(level * -1));
                        if (log.isDebugEnabled()) log.debug("Path {} matched UriTemplate {}", path, uriTemplate);
                        if (log.isDebugEnabled()) log.debug("Path {} matched OntClass {}", path, ontClass);

                        if (!matchedClasses.containsKey(uriTemplate))
                            matchedClasses.put(uriTemplate, new ArrayList<Template>());
                        matchedClasses.get(uriTemplate).add(template);
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
                        Map<UriTemplate, List<Template>> matchedImportClasses = matchTemplates(importedOntology, path, level + 1);
                        Iterator<Entry<UriTemplate, List<Template>>> entries = matchedImportClasses.entrySet().iterator();
                        while (entries.hasNext())
                        {
                            Entry<UriTemplate, List<Template>> entry = entries.next();
                            if (matchedClasses.containsKey(entry.getKey()))
                                matchedClasses.get(entry.getKey()).addAll(entry.getValue());
                            else
                                matchedClasses.put(entry.getKey(), entry.getValue());
                        }
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
    public Template matchTemplate(Ontology ontology, CharSequence path) // throws ConfigurationException
    {
	if (ontology == null) throw new IllegalArgumentException("OntModel cannot be null");
        
        TreeMap<UriTemplate, List<Template>> templateMap = new TreeMap<>(UriTemplate.COMPARATOR);
        templateMap.putAll(matchTemplates(ontology, path, 0));
        if (!templateMap.isEmpty())
        {
            if (log.isDebugEnabled()) log.debug("{} path matched these Templates: {} (selecting the first UriTemplate)", path, templateMap);
            List<Template> matchedTemplates = templateMap.firstEntry().getValue();
            Collections.sort(matchedTemplates, Template.COMPARATOR);
            Collections.reverse(matchedTemplates);
            
            Template match = matchedTemplates.get(0);
            if (log.isDebugEnabled()) log.debug("UriTemplate: {} matched Template: {}", templateMap.firstKey(), match);

            Iterator<Template> it = matchedTemplates.iterator();
            while (it.hasNext())
            {
                Template template = it.next();
                if (!template.equals(match) && Template.COMPARATOR.compare(template, match) == 0)
                    if (log.isDebugEnabled()) log.debug("UriTemplate: {} has conflicting Templates: {} (they are equal to the mathed one)", templateMap.firstKey(), template);
            }
            
            return match;
        }
        
        if (log.isDebugEnabled()) log.debug("Path {} has no Template match in this OntModel", path);
        return null;
    }

    // does this belong to Skolemizer instead?
    public OntClass matchOntClass(Resource resource, OntClass parentClass)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (parentClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        StmtIterator it = resource.listProperties(RDF.type);
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                if (stmt.getObject().isURIResource())
                {
                    OntClass typeClass = parentClass.getOntModel().getOntClass(stmt.getObject().asResource().getURI());
                    // return resource type which is defined by the sitemap ontology
                    if (typeClass != null && typeClass.getIsDefinedBy() != null &&
                            typeClass.getIsDefinedBy().equals(parentClass.getIsDefinedBy()))
                        return typeClass;
                }
            }
        }
        finally
        {
            it.close();
        }

        return null;
    }

    public Map<Property, List<OntClass>> ontClassesByAllValuesFrom(ServletConfig servletConfig, Ontology ontology, Property onProperty, OntClass allValuesFrom) // throws ConfigurationException
    {
	if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");        
	if (ontology == null) throw new IllegalArgumentException("OntModel cannot be null");
        if (allValuesFrom == null) throw new IllegalArgumentException("OntClass cannot be null");

        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add(RDFS.isDefinedBy.getLocalName(), ontology);
        qsm.add(OWL.allValuesFrom.getLocalName(), allValuesFrom);
        if (onProperty != null) qsm.add(OWL.onProperty.getLocalName(), onProperty);

        QueryExecution qex = QueryExecutionFactory.create(getQuery(getQuery(servletConfig, GP.restrictionsQuery), qsm), ontology.getOntModel());
        try
        {
            Map<Property, List<OntClass>> matchedClasses = new HashMap<>();
            ResultSet templates = qex.execSelect();

            while (templates.hasNext())
            {
                QuerySolution solution = templates.next();
                if (solution.contains(GP.Template.getLocalName())) // solution.contains(OWL.onProperty.getLocalName()
                {
                    OntClass template = solution.getResource(GP.Template.getLocalName()).as(OntClass.class);

                    if (!matchedClasses.containsKey(onProperty))
                        matchedClasses.put(onProperty, new ArrayList<OntClass>());
                    matchedClasses.get(onProperty).add(template);
                }
            }

            if (matchedClasses.isEmpty())
            {
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
                            Map<Property, List<OntClass>> matchedImportClasses = ontClassesByAllValuesFrom(servletConfig, importedOntology, onProperty, allValuesFrom);
                            Iterator<Entry<Property, List<OntClass>>> entries = matchedImportClasses.entrySet().iterator();
                            while (entries.hasNext())
                            {
                                Entry<Property, List<OntClass>> entry = entries.next();
                                if (matchedClasses.containsKey(entry.getKey()))
                                    matchedClasses.get(entry.getKey()).addAll(entry.getValue());
                                else
                                    matchedClasses.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
                finally
                {
                    imports.close();
                }
            }

            return matchedClasses;
        }
        finally
        {
            qex.close();
        }
    }
    
    public Query getQuery(ServletConfig servletConfig, DatatypeProperty property)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        
	Object query = servletConfig.getInitParameter(property.getURI());
        if (query == null) throw new ConfigurationException("Property '" + property.getURI() + "' needs to be set in config");
        
        ParameterizedSparqlString queryString = new ParameterizedSparqlString(query.toString());
        return queryString.asQuery();
    }
    
    public OntModel getOntModel()
    {
        return getOntology().getOntModel();
    }

    public Ontology getOntology()
    {
	ContextResolver<Ontology> cr = getProviders().getContextResolver(Ontology.class, null);
	return cr.getContext(Ontology.class);
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public Providers getProviders()
    {
        return providers;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
}