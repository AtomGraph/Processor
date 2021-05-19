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

import com.atomgraph.processor.exception.OntologyException;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.SIOC;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.AllValuesFromRestriction;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.OntClass;
import org.glassfish.jersey.uri.internal.UriTemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder class that can build URIs from templates for RDF resources as well as models.
 * Needs to be initialized with sitemap ontology, ontology class matching request URI, and request URI information.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
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
    
    public URI build(Resource resource)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        
        StmtIterator it = resource.listProperties(RDF.type);
        
        try
        {
            while (it.hasNext())
            {
                Resource type = it.next().getResource(); // will fail if rdf:type object is not a resource
                if (getOntology().getOntModel().getOntResource(type).canAs(OntClass.class))
                {
                    OntClass typeClass = getOntology().getOntModel().getOntResource(type).asClass();
                    OntClass pathClass = getPathClass(typeClass);

                    if (pathClass != null)
                    {
                        final String path = getStringValue(pathClass, LDT.path);

                        OntClass fragmentClass = getFragmentClass(typeClass);
                        final String fragment;
                        if (fragmentClass != null) fragment = getStringValue(fragmentClass, LDT.fragment);
                        else fragment = null;

                        return build(resource, getUriBuilder(path, resource, typeClass), path, fragment);
                    }
                }
            }
        }
        finally
        {
            it.close();
        }
        
        return null;
    }
    
    public UriBuilder getUriBuilder(String path, Resource resource, OntClass typeClass)
    {
        if (path == null) throw new IllegalArgumentException("Path cannot be null");
        if (typeClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        final UriBuilder builder;
        // treat paths starting with / as absolute, others as relative (to the current absolute path)
        // JAX-RS URI templates do not have this distinction (leading slash is irrelevant)
        if (path.startsWith("/"))
            builder = getBaseUriBuilder().clone();
        else
        {
            Resource parent = getParent(typeClass);
            if (parent != null) builder = UriBuilder.fromUri(parent.getURI());
            else builder = getAbsolutePathBuilder().clone();
        }
        
        return builder;
    }
    
    public URI build(Resource resource, UriBuilder builder, String path, String fragment)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (builder == null) throw new IllegalArgumentException("UriBuilder cannot be null");

        Map<String, String> nameValueMap = getNameValueMap(resource, new UriTemplateParser(path));
        return builder.path(path).fragment(fragment).buildFromMap(nameValueMap); // TO-DO: wrap into SkolemizationException
    }

    public static Map<String, String> getNameValueMap(Resource resource, UriTemplateParser parser)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (parser == null) throw new IllegalArgumentException("UriTemplateParser cannot be null");

        Map<String, String> nameValueMap = new HashMap<>();
        
        List<String> names = parser.getNames();
        for (String name : names)
        {
            Literal literal = getLiteral(resource, name);
            if (literal != null) nameValueMap.put(name, literal.getString());
        }

        return nameValueMap;
    }

    public static Literal getLiteral(Resource resource, String namePath)
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

    public static Resource getResource(Resource resource, String name)
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

    public OntClass getPathClass(OntClass ontClass)
    {
        return getPathClass(ontClass, getStringValue(ontClass, LDT.path));
    }
    
    public OntClass getPathClass(OntClass ontClass, String path)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        
        if (path != null) return ontClass;
        else
        {
            ExtendedIterator<OntClass> it = ontClass.listSuperClasses();
            try
            {
                while (it.hasNext())
                {
                    OntClass superClass = it.next();
                    OntClass pathClass = getPathClass(superClass);
                    if (pathClass != null) return pathClass;
                }
            }
            finally
            {
                it.close();
            }
        }
        
        return null;
    }
    
    public OntClass getFragmentClass(OntClass ontClass)
    {
        return getFragmentClass(ontClass, getStringValue(ontClass, LDT.fragment));
    }
    
    public OntClass getFragmentClass(OntClass ontClass, String fragment)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        
        if (fragment != null) return ontClass;
        else
        {
            ExtendedIterator<OntClass> it = ontClass.listSuperClasses();
            try
            {
                while (it.hasNext())
                {
                    OntClass superClass = it.next();
                    OntClass fragmentClass = getFragmentClass(superClass);
                    if (fragmentClass != null) return fragmentClass;
                }
            }
            finally
            {
                it.close();
            }
        }
        
        return null;
    }
    
    protected String getStringValue(OntClass ontClass, Property property)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        if (ontClass.hasProperty(property))
        {
            if (!ontClass.getPropertyValue(property).isLiteral() ||
                    ontClass.getPropertyValue(property).asLiteral().getDatatype() == null ||
                    !ontClass.getPropertyValue(property).asLiteral().getDatatype().equals(XSDDatatype.XSDstring))
            {
                if (log.isErrorEnabled()) log.error("Class {} property {} is not an xsd:string literal", ontClass, property);
                throw new OntologyException("Class '" + ontClass + "' property '" + property + "' is not an xsd:string literal");
            }
            
            return ontClass.getPropertyValue(property).asLiteral().getString();
        }
        
        return null;
    }

    // TO-DO: move to a LDTDH (document hierarchy) specific Skolemizer subclass
    public Resource getParent(OntClass ontClass)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        ExtendedIterator<OntClass> hasValueIt = ontClass.listSuperClasses();
        try
        {
            while (hasValueIt.hasNext()) // TO-DO: catch org.apache.jena.ontology.ConversionException
            {
                OntClass superClass = hasValueIt.next();
                
                if (superClass.canAs(HasValueRestriction.class))
                {
                    HasValueRestriction hvr = superClass.as(HasValueRestriction.class);
                    if (hvr.getOnProperty().equals(SIOC.HAS_PARENT) || hvr.getOnProperty().equals(SIOC.HAS_CONTAINER))
                    {
                        if (!hvr.getHasValue().isURIResource())
                        {
                            if (log.isErrorEnabled()) log.error("HasValue restriction on class {} for property {} is not a URI resource", ontClass, hvr.getOnProperty());
                            throw new OntologyException("HasValue restriction on class '" + ontClass + "' for property '" + hvr.getOnProperty() + "' is not a URI resource");
                        }
                        
                        return hvr.getHasValue().asResource();
                    }
                }
            }
        }
        catch (ConversionException ex)
        {
            if (log.isErrorEnabled()) log.error("Class '{}' has invalid (unresolved) superclass. Check if all superclass ontologies are imported", ontClass);
            throw new OntologyException(ex);
        }
        finally
        {
            hasValueIt.close();
        }
        
        ExtendedIterator<OntClass> allValuesFromIt = ontClass.listSuperClasses();
        try
        {
            while (allValuesFromIt.hasNext())
            {
                OntClass superClass = allValuesFromIt.next();
                
                if (superClass.canAs(AllValuesFromRestriction.class))
                {
                    AllValuesFromRestriction avr = superClass.as(AllValuesFromRestriction.class);
                    if (!avr.getAllValuesFrom().canAs(OntClass.class))
                    {
                        if (log.isErrorEnabled()) log.error("AllValuesFrom restriction on class {} for property {} is not an OntClass resource", ontClass, avr.getOnProperty());
                        throw new OntologyException("AllValuesFrom restriction on class '" + ontClass + "' for property '" + avr.getOnProperty() + "' is not an OntClass resource");
                    }

                    OntClass valueClass = avr.getAllValuesFrom().as(OntClass.class);
                    return getParent(valueClass);
                }
            }
        }
        catch (ConversionException ex)
        {
            if (log.isErrorEnabled()) log.error("Class '{}' has invalid (unresolved) superclass. Check if all superclass ontologies are imported", ontClass);
            throw new OntologyException(ex);
        }
        finally
        {
            allValuesFromIt.close();
        }
        
        return null;
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
