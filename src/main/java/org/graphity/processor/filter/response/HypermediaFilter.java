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

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.model.impl.ConstructorBase;
import org.graphity.core.util.StateBuilder;
import org.graphity.processor.exception.ConstraintViolationException;
import org.graphity.processor.util.RestrictionMatcher;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.SIOC;
import org.graphity.processor.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.util.JenaUtil;
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
        
        if (getResource() != null && response.getEntity() != null &&
                (response.getEntity() instanceof Model) || response.getEntity() instanceof ConstraintViolationException)
        {
            Model model;
            if (response.getEntity() instanceof ConstraintViolationException)
                model = ((ConstraintViolationException)response.getEntity()).getModel();
            else
                model = (Model)response.getEntity();
            long oldCount = model.size();
            
            Resource resource = model.createResource(request.getAbsolutePath().toString());
            // TO-DO: remove dependency on matched class. The filter should operate solely on the in-band RDF response
            model = addStates(resource, getResource().getMatchedOntClass());
            
            if (getResource().getForClass() != null && !(response.getEntity() instanceof ConstraintViolationException))
            {
                OntClass forClass = getOntology().getOntModel().createClass(getResource().getForClass().getURI());
                model = addInstance(model, forClass);
            }
            
            if (log.isDebugEnabled()) log.debug("Added HATEOAS transitions to the response RDF Model for resource: {} # of statements: {}", resource.getURI(), model.size() - oldCount);
            response.setEntity(model);
            return response;
        }
        
        return response;
    }

    public Model addInstance(Model model, OntClass forClass)
    {
        if (forClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        Property property = SPIN.constructor;
        if (log.isDebugEnabled()) log.debug("Invoking constructor on class {} using property {}", forClass, property);
        new ConstructorBase().construct(forClass, property, model);
        
        return model;
    }
    
    public StateBuilder getStateBuilder(Resource resource)
    {
        StateBuilder sb = StateBuilder.fromUri(resource.getURI(), resource.getModel());
        
        if (getResource().getLimit() != null) sb.replaceLiteral(GP.limit, getResource().getLimit());
        if (getResource().getOffset() != null) sb.replaceLiteral(GP.offset, getResource().getOffset());
        if (getResource().getOrderBy() != null) sb.replaceLiteral(GP.orderBy, getResource().getOrderBy());
        if (getResource().getDesc() != null) sb.replaceLiteral(GP.desc, getResource().getDesc());
        
        return sb;
    }
    
    public Model addStates(Resource resource, OntClass matchedOntClass)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (matchedOntClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        
        // Adding type to description does not work with GSP responses. Can be retrieved from Link header instead.
        // resource.addProperty(RDF.type, matchedOntClass);
        
	if (matchedOntClass.equals(GP.Container) || hasSuperClass(matchedOntClass, GP.Container))
	{
            if (getResource().getForClass() != null)
            {
                StateBuilder.fromResource(resource).
                    replaceProperty(GP.forClass, getResource().getForClass()).
                    build().
                    //addProperty(RDF.type, FOAF.Document).
                    addProperty(RDF.type, GP.Constructor).
                    addProperty(GP.constructorOf, resource);
            }
            else
            {
                Map<Property, List<OntClass>> childrenClasses = new HashMap<>();
                Query restrictionQuery = ((org.graphity.processor.Application)getApplication()).getQuery(GP.restrictionsQuery);
                RestrictionMatcher restrictionMatcher = new RestrictionMatcher(getOntology(), restrictionQuery);
                childrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_PARENT, matchedOntClass));
                childrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_CONTAINER, matchedOntClass));
                for (Resource superCls : JenaUtil.getAllSuperClasses(matchedOntClass))
                    if (superCls.isURIResource() && superCls.canAs(OntClass.class))
                    {
                        childrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_PARENT, superCls.as(OntClass.class)));
                        childrenClasses.putAll(restrictionMatcher.match(SIOC.HAS_CONTAINER, superCls.as(OntClass.class)));
                    }
                
                Iterator<List<OntClass>> it = childrenClasses.values().iterator();
                while (it.hasNext())
                {
                    List<OntClass> forClasses = it.next();
                    Iterator<OntClass> forIt = forClasses.iterator();
                    while (forIt.hasNext())
                    {
                        OntClass forClass = forIt.next();
                        StateBuilder.fromResource(resource).
                            replaceProperty(GP.forClass, forClass).
                            build().
                            //addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Constructor).
                            addProperty(GP.constructorOf, resource);                            
                    }
                }
                
                Resource page = getStateBuilder(resource).build().
                    addProperty(GP.pageOf, resource).
                    //addProperty(RDF.type, FOAF.Document).
                    addProperty(RDF.type, GP.Page);
                if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} gp:pageOf {}", page, resource);

                if (getResource().getLimit() != null)
                {
                    Long offset = getResource().getOffset();
                    if (offset == null) offset = Long.valueOf(0);
                    
                    if (offset >= getResource().getLimit())
                    {
                        Resource prev = getStateBuilder(resource).
                            replaceLiteral(GP.offset, offset - getResource().getLimit()).
                            build().
                            addProperty(GP.pageOf, resource).
                            //addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page).
                            addProperty(XHV.next, page);

                        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                        page.addProperty(XHV.prev, prev);
                    }

                    // no way to know if there's a next page without counting results (either total or in current page)
                    //int subjectCount = describe().listSubjects().toList().size();
                    //log.debug("describe().listSubjects().toList().size(): {}", subjectCount);
                    //if (subjectCount >= getResource().getLimit())
                    {
                        Resource next = getStateBuilder(resource).
                            replaceLiteral(GP.offset, offset + getResource().getLimit()).
                            build().
                            addProperty(GP.pageOf, resource).
                            //addProperty(RDF.type, FOAF.Document).
                            addProperty(RDF.type, GP.Page).
                            addProperty(XHV.prev, page);

                        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
                        page.addProperty(XHV.next, next);
                    }
                }
            }
        }
        
        return resource.getModel();
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
        return getResource().getMatchedOntClass();
    }

    public Ontology getOntology()
    {
        return getResource().getOntology();
    }

    public Application getApplication()
    {
        return application;
    }
    
}
