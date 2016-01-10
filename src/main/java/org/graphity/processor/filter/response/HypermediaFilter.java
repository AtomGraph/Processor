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

package org.graphity.processor.filter.response;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.model.impl.ConstructorBase;
import org.graphity.processor.util.OntClassMatcher;
import org.graphity.processor.util.Modifiers;
import org.graphity.core.util.StateBuilder;
import org.graphity.processor.Application;
import org.graphity.processor.util.RestrictionMatcher;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.SIOC;
import org.graphity.processor.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SPIN;

/**
 * A filter that adds HATEOAS transitions to the RDF query result.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{
    private static final Logger log = LoggerFactory.getLogger(HypermediaFilter.class);
    
    @Context Application application;
    @Context Providers providers;
    @Context UriInfo uriInfo;
    @Context ServletConfig servletConfig;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        if (getResource() != null && response.getStatusType().getFamily().equals(Family.SUCCESSFUL) &&
                response.getEntity() != null && response.getEntity() instanceof Model)
        {
            Model model = (Model)response.getEntity();
            long oldCount = model.size();
            Resource resource = model.createResource(request.getAbsolutePath().toString());
            OntClass matchedOntClass = getResource().getMatchedOntClass();
            model = addStates(resource, matchedOntClass);
            if (log.isDebugEnabled()) log.debug("Added HATEOAS transitions to the response RDF Model for resource: {} # of statements: {}", resource.getURI(), model.size() - oldCount);
            response.setEntity(model);
            return response;
        }
        
        return response;
    }

    public Model addStates(com.hp.hpl.jena.rdf.model.Resource resource, OntClass matchedOntClass)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (matchedOntClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        
        Model model = resource.getModel();
        
	if (matchedOntClass.equals(GP.Container) || hasSuperClass(matchedOntClass, GP.Container))
	{
            Map<Property, List<OntClass>> childrenClasses = new HashMap<>();
            Query restrictionQuery = getApplication().getQuery(GP.restrictionsQuery);
            RestrictionMatcher restrictionMatcher = new RestrictionMatcher(getOntology(), restrictionQuery);
            childrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_PARENT, matchedOntClass));
            childrenClasses.putAll(restrictionMatcher.match(getOntology(), SIOC.HAS_CONTAINER, matchedOntClass));

            Iterator<List<OntClass>> it = childrenClasses.values().iterator();
            while (it.hasNext())
            {
                List<OntClass> forClasses = it.next();
                Iterator<OntClass> forIt = forClasses.iterator();
                while (forIt.hasNext())
                {
                    OntClass forClass = forIt.next();
                    StateBuilder.fromUri(resource.getURI(), model).
                            property(GP.forClass, forClass).
                            property(GP.mode, GP.ConstructMode).
                            build().
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Constructor).
                            addProperty(GP.constructorOf, resource);                            
                }
            }
            
            ResIterator resIt = model.listResourcesWithProperty(SIOC.HAS_PARENT, resource);
            while (resIt.hasNext())
            {
                com.hp.hpl.jena.rdf.model.Resource childContainer = resIt.next();
                URI childURI = URI.create(childContainer.getURI());
                OntClass childClass = new OntClassMatcher(getOntology()).match(childURI, getUriInfo().getBaseUri());
                Map<Property, List<OntClass>> grandChildrenClasses = new HashMap<>();
                grandChildrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_PARENT, childClass));
                grandChildrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_CONTAINER, childClass));

                Iterator<List<OntClass>> gccIt = grandChildrenClasses.values().iterator();
                while (gccIt.hasNext())
                {
                    List<OntClass> forClasses = gccIt.next();
                    Iterator<OntClass> forIt = forClasses.iterator();
                    while (forIt.hasNext())
                    {
                        OntClass forClass = forIt.next();
                        StateBuilder.fromUri(childContainer.getURI(), model).
                                property(GP.forClass, forClass).
                                property(GP.mode, GP.ConstructMode).
                                build().
                                addProperty(RDF.type, FOAF.Document).
                                addProperty(RDF.type, GP.Constructor).
                                addProperty(GP.constructorOf, childContainer);
                    }
                }
            }

            if (getResource().getMode() != null && getResource().getMode().equals(GP.ConstructMode))
            {
                try
                {
                    if (!getUriInfo().getQueryParameters().containsKey(GP.forClass.getLocalName()))
                        throw new IllegalStateException("gp:ConstructMode is active, but gp:forClass value not supplied");

                    URI forClassURI = new URI(getUriInfo().getQueryParameters().getFirst(GP.forClass.getLocalName()));
                    OntClass forClass = getOntology().getOntModel().createClass(forClassURI.toString());
                    if (forClass == null) throw new IllegalStateException("gp:ConstructMode is active, but gp:forClass value is not a known owl:Class");

                    Property property = SPIN.constructor;
                    if (log.isDebugEnabled()) log.debug("Invoking constructor on class {} using property {}", forClass, property);
                    new ConstructorBase().construct(forClass, property, model);
                }
                catch (URISyntaxException ex)
                {
                    if (log.isErrorEnabled()) log.error("gp:ConstructMode is active but gp:forClass value is not a URI: '{}'", getUriInfo().getQueryParameters().getFirst(GP.forClass.getLocalName()));
                    throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                }
            }
            else
            {
                StateBuilder sb = StateBuilder.fromUri(resource.getURI(), model);
                if (getModifiers().getLimit() != null) sb.literal(GP.limit, getModifiers().getLimit());
                if (getModifiers().getOffset() != null) sb.literal(GP.offset, getModifiers().getOffset());
                if (getModifiers().getOrderBy() != null) sb.literal(GP.orderBy, getModifiers().getOrderBy());
                if (getModifiers().getDesc() != null) sb.literal(GP.desc, getModifiers().getDesc());
                if (getResource().getMode() != null) sb.property(GP.mode, getResource().getMode());
                com.hp.hpl.jena.rdf.model.Resource page = sb.build().
                        addProperty(GP.pageOf, resource).
                        addProperty(RDF.type, FOAF.Document).
                        addProperty(RDF.type, GP.Page);
                if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} gp:pageOf {}", page, resource);

                if (getModifiers().getOffset() != null && getModifiers().getLimit() != null)
                {
                    if (getModifiers().getOffset() >= getModifiers().getLimit())
                    {
                        StateBuilder prevSb = StateBuilder.fromUri(resource.getURI(), model).
                                literal(GP.limit, getModifiers().getLimit()).
                                literal(GP.offset, getModifiers().getOffset() - getModifiers().getLimit());
                        if (getModifiers().getOrderBy() != null) prevSb.literal(GP.orderBy, getModifiers().getOrderBy());
                        if (getModifiers().getDesc() != null) prevSb.literal(GP.desc, getModifiers().getDesc());
                        if (getResource().getMode() != null) prevSb.property(GP.mode, getResource().getMode());
                        com.hp.hpl.jena.rdf.model.Resource prev = prevSb.build().
                            addProperty(GP.pageOf, resource).
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page).
                            addProperty(XHV.next, page);

                        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                        page.addProperty(XHV.prev, prev);
                    }

                    // no way to know if there's a next page without counting results (either total or in current page)
                    //int subjectCount = describe().listSubjects().toList().size();
                    //log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
                    //if (subjectCount >= getModifiers().getLimit())
                    {
                        StateBuilder nextSb = StateBuilder.fromUri(resource.getURI(), model).
                                literal(GP.limit, getModifiers().getLimit()).
                                literal(GP.offset, getModifiers().getOffset() + getModifiers().getLimit());
                        if (getModifiers().getOrderBy() != null) nextSb.literal(GP.orderBy, getModifiers().getOrderBy());
                        if (getModifiers().getDesc() != null) nextSb.literal(GP.desc, getModifiers().getDesc());
                        if (getResource().getMode() != null) nextSb.property(GP.mode, getResource().getMode());
                        com.hp.hpl.jena.rdf.model.Resource next = nextSb.build().
                            addProperty(GP.pageOf, resource).
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page).
                            addProperty(XHV.prev, page);

                        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
                        page.addProperty(XHV.next, next);
                    }
                }
            }
        }
        
        return model;
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
    
    public Providers getProviders()
    {
        return providers;
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    public org.graphity.processor.model.Resource getResource()
    {
        if (!getUriInfo().getMatchedResources().isEmpty())
        {
            Object resource = getUriInfo().getMatchedResources().get(0);
            if (resource instanceof org.graphity.processor.model.Resource) return (org.graphity.processor.model.Resource)resource;
        }
        
        return null;
    }
    
    public OntClass getMatchedOntClass()
    {
	//return getProviders().getContextResolver(OntClass.class, null).getContext(OntClass.class);
        return getResource().getMatchedOntClass();
    }

    public Ontology getOntology()
    {
	//return getProviders().getContextResolver(Ontology.class, null).getContext(Ontology.class);
        return getResource().getOntology();
    }

    public Modifiers getModifiers()
    {
	return getProviders().getContextResolver(Modifiers.class, null).getContext(Ontology.class);
        //return getResource().getModifiers();
    }

    public Application getApplication()
    {
        return application;
    }
    
}
