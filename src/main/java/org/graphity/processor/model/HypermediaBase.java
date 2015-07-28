/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.model;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.provider.OntClassMatcher;
import org.graphity.processor.util.Modifiers;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.SIOC;
import org.graphity.processor.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.vocabulary.SP;

/**
 * HATEOAS: hypermedia as the engine of application state.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
public class HypermediaBase
{

    private static final Logger log = LoggerFactory.getLogger(HypermediaBase.class);

    private final ServletConfig servletConfig;
    private final UriInfo uriInfo;
    private final Modifiers modifiers;
    private final OntClass matchedOntClass;
    private final Ontology ontology;
    private final URI mode;
    
    public HypermediaBase(ServletConfig servletConfig, UriInfo uriInfo, Modifiers modifiers, Ontology ontology, OntClass matchedOntClass)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (uriInfo == null) throw new IllegalArgumentException("UriInfo cannot be null");
        if (modifiers == null) throw new IllegalArgumentException("Modifiers cannot be null");
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (matchedOntClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        
        this.servletConfig = servletConfig;
        this.uriInfo = uriInfo;
        this.modifiers = modifiers;
        this.ontology = ontology;
        this.matchedOntClass = matchedOntClass;
        if (uriInfo.getQueryParameters().containsKey(GP.mode.getLocalName()))
            this.mode = URI.create(uriInfo.getQueryParameters().getFirst(GP.mode.getLocalName()));
        else mode = null;
    }
    
    protected UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    protected OntClass getMatchedOntClass()
    {
        return matchedOntClass;
    }

    protected ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    public Modifiers getModifiers()
    {
        return modifiers;
    }
    
    protected URI getMode()
    {
        return mode;
    }

    public Ontology getOntology()
    {
        return ontology;
    }
    
    public Model addStates(Resource resource, Model model)
    {
        // mixing sitemap and description traversal - a good idea??
        
	if (getMatchedOntClass().equals(GP.Container) || hasSuperClass(getMatchedOntClass(), GP.Container))
	{
            Map<Property, List<OntClass>> childrenClasses = new HashMap<>();
            childrenClasses.putAll(new OntClassMatcher().ontClassesByAllValuesFrom(getServletConfig(), getOntology(), SIOC.HAS_PARENT, getMatchedOntClass()));
            childrenClasses.putAll(new OntClassMatcher().ontClassesByAllValuesFrom(getServletConfig(), getOntology(), SIOC.HAS_CONTAINER, getMatchedOntClass()));

            Iterator<List<OntClass>> it = childrenClasses.values().iterator();
            while (it.hasNext())
            {
                List<OntClass> forClasses = it.next();
                Iterator<OntClass> forIt = forClasses.iterator();
                while (forIt.hasNext())
                {
                    OntClass forClass = forIt.next();
                    String constructorURI = getStateUriBuilder(UriBuilder.fromUri(resource.getURI()), null, null, null, null, URI.create(GP.ConstructMode.getURI())).
                            queryParam(GP.forClass.getLocalName(), forClass.getURI()).build().toString();
                        com.hp.hpl.jena.rdf.model.Resource template = createState(model.createResource(constructorURI), null, null, null, null, GP.ConstructMode).
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Constructor).
                            addProperty(GP.forClass, forClass).
                            addProperty(GP.constructorOf, resource);
                }
            }
            
            ResIterator resIt = model.listResourcesWithProperty(SIOC.HAS_PARENT, resource);
            while (resIt.hasNext())
            {
                com.hp.hpl.jena.rdf.model.Resource childContainer = resIt.next();
                URI childURI = URI.create(childContainer.getURI());
                OntClass childClass = new OntClassMatcher().matchOntClass(getServletConfig(), getOntology(), childURI, getUriInfo().getBaseUri());
                Map<Property, List<OntClass>> grandChildrenClasses = new HashMap<>();
                grandChildrenClasses.putAll(new OntClassMatcher().ontClassesByAllValuesFrom(getServletConfig(), getOntology(), SIOC.HAS_PARENT, childClass));
                grandChildrenClasses.putAll(new OntClassMatcher().ontClassesByAllValuesFrom(getServletConfig(), getOntology(), SIOC.HAS_CONTAINER, childClass));

                Iterator<List<OntClass>> gccIt = grandChildrenClasses.values().iterator();
                while (gccIt.hasNext())
                {
                    List<OntClass> forClasses = gccIt.next();
                    Iterator<OntClass> forIt = forClasses.iterator();
                    while (forIt.hasNext())
                    {
                        OntClass forClass = forIt.next();
                        String constructorURI = getStateUriBuilder(UriBuilder.fromUri(childURI), null, null, null, null, URI.create(GP.ConstructMode.getURI())).
                            queryParam(GP.forClass.getLocalName(), forClass.getURI()).build().toString();
                        com.hp.hpl.jena.rdf.model.Resource template = createState(model.createResource(constructorURI), null, null, null, null, GP.ConstructMode).
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Constructor).
                            addProperty(GP.forClass, forClass).                                    
                            addProperty(GP.constructorOf, childContainer);
                    }
                }
            }

            if (getMode() != null && getMode().equals(URI.create(GP.ConstructMode.getURI())))
            {
                try
                {
                    if (!getUriInfo().getQueryParameters().containsKey(GP.forClass.getLocalName()))
                        throw new IllegalStateException("gp:ConstructMode is active, but gp:forClass value not supplied");

                    URI forClassURI = new URI(getUriInfo().getQueryParameters().getFirst(GP.forClass.getLocalName()));
                    OntClass forClass = getOntology().getOntModel().createClass(forClassURI.toString());
                    if (forClass == null) throw new IllegalStateException("gp:ConstructMode is active, but gp:forClass value is not a known owl:Class");

                    Query templateQuery = getQuery(forClass, GP.template);
                    if (templateQuery == null)
                    {
                        if (log.isErrorEnabled()) log.error("gp:ConstructMode is active but template not defined for class '{}' (gp:template missing)", forClass.getURI());
                        throw new SitemapException("gp:ConstructMode template not defined for class '" + forClass.getURI() +"'");
                    }
                    
                    QueryExecution qex = QueryExecutionFactory.create(templateQuery, ModelFactory.createDefaultModel());
                    Model templateModel = qex.execConstruct();
                    model.add(templateModel);
                    if (log.isDebugEnabled()) log.debug("gp:template CONSTRUCT query '{}' created {} triples", templateQuery, templateModel.size());
                    qex.close();
                }
                catch (URISyntaxException ex)
                {
                    if (log.isErrorEnabled()) log.error("gp:ConstructMode is active but gp:forClass value is not a URI: '{}'", getUriInfo().getQueryParameters().getFirst(GP.forClass.getLocalName()));
                    throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                }
            }
            else
            {
                if (getModifiers().getLimit() != null)
                {
                    if (log.isDebugEnabled()) log.debug("Adding Page metadata: gp:pageOf {}", resource);
                    String pageURI = getStateUriBuilder(UriBuilder.fromUri(resource.getURI()), getModifiers().getOffset(), getModifiers().getLimit(), getModifiers().getOrderBy(), getModifiers().getDesc(), null).build().toString();
                    com.hp.hpl.jena.rdf.model.Resource page = createState(model.createResource(pageURI), getModifiers().getOffset(), getModifiers().getLimit(), getModifiers().getOrderBy(), getModifiers().getDesc(), null).
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page).
                            addProperty(GP.pageOf, resource);

                    if (getModifiers().getOffset() != null && getModifiers().getLimit() != null)
                    {
                        if (getModifiers().getOffset() >= getModifiers().getLimit())
                        {
                            String prevURI = getStateUriBuilder(UriBuilder.fromUri(resource.getURI()), getModifiers().getOffset() - getModifiers().getLimit(), getModifiers().getLimit(), getModifiers().getOrderBy(), getModifiers().getDesc(), getMode()).build().toString();
                            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", resource.getURI(), prevURI);
                            page.addProperty(XHV.prev, model.createResource(prevURI));
                        }

                        // no way to know if there's a next page without counting results (either total or in current page)
                        //int subjectCount = describe().listSubjects().toList().size();
                        //log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
                        //if (subjectCount >= getModifiers().getLimit())
                        {
                            String nextURI = getStateUriBuilder(UriBuilder.fromUri(resource.getURI()), getModifiers().getOffset() + getModifiers().getLimit(), getModifiers().getLimit(), getModifiers().getOrderBy(), getModifiers().getDesc(), getMode()).build().toString();
                            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", resource.getURI(), nextURI);
                            page.addProperty(XHV.next, model.createResource(nextURI));
                        }
                    }
                }
            }
        }
        
        return model;
    }
    
    /**
     * Creates a page resource for the current container. Includes HATEOS previous/next links.
     * 
     * @param state
     * @param offset
     * @param limit
     * @param orderBy
     * @param desc
     * @param mode
     * @return page resource
     */
    public com.hp.hpl.jena.rdf.model.Resource createState(com.hp.hpl.jena.rdf.model.Resource state, Long offset, Long limit, String orderBy, Boolean desc, com.hp.hpl.jena.rdf.model.Resource mode)
    {
        if (state == null) throw new IllegalArgumentException("Resource subject cannot be null");        

        if (offset != null) state.addLiteral(GP.offset, offset);
        if (limit != null) state.addLiteral(GP.limit, limit);
        if (orderBy != null) state.addLiteral(GP.orderBy, orderBy);
        if (desc != null) state.addLiteral(GP.desc, desc);
        if (mode != null) state.addProperty(GP.mode, mode);
        
        return state;
    }

    /**
     * Returns URI builder instantiated with pagination parameters for the current page.
     * 
     * @param uriBuilder
     * @param offset
     * @param limit
     * @param orderBy
     * @param desc
     * @param mode
     * @return URI builder
     */    
    public UriBuilder getStateUriBuilder(UriBuilder uriBuilder, Long offset, Long limit, String orderBy, Boolean desc, URI mode)
    {        
        if (uriBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");        

        if (offset != null) uriBuilder.queryParam(GP.offset.getLocalName(), offset);
        if (limit != null) uriBuilder.queryParam(GP.limit.getLocalName(), limit);
	if (orderBy != null) uriBuilder.queryParam(GP.orderBy.getLocalName(), orderBy);
	if (desc != null) uriBuilder.queryParam(GP.desc.getLocalName(), desc);
        if (mode != null) uriBuilder.queryParam(GP.mode.getLocalName(), mode);
        
	return uriBuilder;
    }
 
    public boolean hasSuperClass(OntClass subClass, OntClass superClass)
    {
        ExtendedIterator<OntClass> it = subClass.listSuperClasses(false);
        
        try
        {
            while (it.hasNext())
            {
                OntClass nextClass = it.next();
                if (nextClass.equals(superClass) || hasSuperClass(nextClass, superClass)) return true;
            }
        }
        finally
        {
            it.close();
        }
        
        return false;
    }
    
    public Query getQuery(OntClass ontClass, AnnotationProperty property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

	com.hp.hpl.jena.rdf.model.Resource queryOrTemplateCall = ontClass.getPropertyResourceValue(property);
        if (queryOrTemplateCall != null)
        {
            // workaround SPIN API which does not parse query from resource if it has properties other than sp:text
            // https://groups.google.com/forum/#!topic/topbraid-users/AVXXEJdbQzk
            Model queryModel = ModelFactory.createDefaultModel();
            Literal queryString = queryOrTemplateCall.getRequiredProperty(SP.text).getLiteral();
            com.hp.hpl.jena.rdf.model.Resource temp = queryModel.createResource().addLiteral(SP.text, queryString);
            StmtIterator it = queryOrTemplateCall.listProperties(RDF.type);
            while (it.hasNext())
            {
                Statement stmt = it.next();
                temp.addProperty(RDF.type, stmt.getObject());
            }
            queryOrTemplateCall = temp;

            org.topbraid.spin.model.Query spinQuery = SPINFactory.asQuery(queryOrTemplateCall);
            if (spinQuery != null) return new ParameterizedSparqlString(spinQuery.toString()).asQuery();

            TemplateCall templateCall = SPINFactory.asTemplateCall(queryOrTemplateCall);
            if (templateCall != null) return new ParameterizedSparqlString(templateCall.getQueryString()).asQuery();
        }
        
        return null;
    }
    
    public Model getConstructedModel(Query query, Model model)
    {
	if (query == null) throw new IllegalArgumentException("Query cannot be null");
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (!query.isConstructType()) throw new IllegalArgumentException("Query is not CONSTRUCT");
        
        QueryExecution qex = QueryExecutionFactory.create(query, model);
        Model result = qex.execConstruct();
        qex.close();
        
        return result;
    }
    
    public Model getConstructedModel(Query query)
    {
        return getConstructedModel(query, ModelFactory.createDefaultModel());
    }

    public Model getConstructedModel(String queryString)
    {
        return getConstructedModel(QueryFactory.create(queryString));
    }

}