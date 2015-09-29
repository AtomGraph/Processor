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

package org.graphity.processor.model.impl;

import org.graphity.processor.util.StateBuilder;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
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
import javax.ws.rs.core.UriInfo;
import org.graphity.processor.model.Hypermedia;
import org.graphity.processor.provider.OntClassMatcher;
import org.graphity.processor.util.Modifiers;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.SIOC;
import org.graphity.processor.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SPIN;

/**
 * HATEOAS: hypermedia as the engine of application state.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
public class HypermediaBase implements Hypermedia
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
    
    @Override
    public Model addStates(com.hp.hpl.jena.rdf.model.Resource resource, Model model)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");

	if (log.isDebugEnabled()) log.debug("OntResource {} gets type of OntClass: {}", this, getMatchedOntClass());
	resource.addProperty(RDF.type, getMatchedOntClass());
        
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
                    StateBuilder.fromUri(resource.getURI(), resource.getModel()).
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
                        StateBuilder.fromUri(childContainer.getURI(), childContainer.getModel()).
                                property(GP.forClass, forClass).
                                property(GP.mode, GP.ConstructMode).
                                build().
                                addProperty(RDF.type, FOAF.Document).
                                addProperty(RDF.type, GP.Constructor).
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
                if (getModifiers().getLimit() != null)
                {
                    StateBuilder sb = StateBuilder.fromUri(resource.getURI(), resource.getModel()).
                            literal(GP.limit, getModifiers().getLimit());
                    if (getModifiers().getOffset() != null) sb.literal(GP.offset, getModifiers().getOffset());
                    if (getModifiers().getOrderBy() != null) sb.literal(GP.orderBy, getModifiers().getOrderBy());
                    if (getModifiers().getDesc() != null) sb.literal(GP.desc, getModifiers().getDesc());
                    if (getMode() != null) sb.property(GP.mode, ResourceFactory.createResource(getMode().toString()));
                    com.hp.hpl.jena.rdf.model.Resource page = sb.property(GP.pageOf, resource).
                            build().
                            addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page);
                    if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} gp:pageOf {}", page, resource);
                    
                    if (getModifiers().getOffset() != null && getModifiers().getLimit() != null)
                    {
                        if (getModifiers().getOffset() >= getModifiers().getLimit())
                        {
                            StateBuilder prevSb = StateBuilder.fromUri(resource.getURI(), resource.getModel()).
                                    literal(GP.offset, getModifiers().getOffset() - getModifiers().getLimit()).
                                    literal(GP.limit, getModifiers().getLimit());
                            if (getModifiers().getOrderBy() != null) prevSb.literal(GP.orderBy, getModifiers().getOrderBy());
                            if (getModifiers().getDesc() != null) prevSb.literal(GP.desc, getModifiers().getDesc());
                            if (getMode() != null) prevSb.property(GP.mode, ResourceFactory.createResource(getMode().toString()));
                            com.hp.hpl.jena.rdf.model.Resource prev = prevSb.build();
                            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                            page.addProperty(XHV.prev, prev);
                        }

                        // no way to know if there's a next page without counting results (either total or in current page)
                        //int subjectCount = describe().listSubjects().toList().size();
                        //log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
                        //if (subjectCount >= getModifiers().getLimit())
                        {
                            StateBuilder nextSb = StateBuilder.fromUri(resource.getURI(), resource.getModel()).
                                    literal(GP.offset, getModifiers().getOffset() + getModifiers().getLimit()).
                                    literal(GP.limit, getModifiers().getLimit());
                            if (getModifiers().getOrderBy() != null) nextSb.literal(GP.orderBy, getModifiers().getOrderBy());
                            if (getModifiers().getDesc() != null) nextSb.literal(GP.desc, getModifiers().getDesc());
                            if (getMode() != null) nextSb.property(GP.mode, ResourceFactory.createResource(getMode().toString()));
                            com.hp.hpl.jena.rdf.model.Resource next = nextSb.build();

                            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
                            page.addProperty(XHV.next, next);
                        }
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
    
}