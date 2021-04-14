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

package com.atomgraph.server.mapper;

import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.util.Link;
import com.atomgraph.core.util.ModelUtils;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.vocabulary.HTTP;
import java.net.URI;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.jena.query.Dataset;

/**
 * Abstract base class for ExceptionMappers that build responses with exceptions as RDF resources.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
abstract public class ExceptionMapperBase
{

    @Context private Request request;
    @Context private UriInfo uriInfo;
    
    private final Optional<Ontology> ontology;
    private final Optional<TemplateCall> templateCall;
    private final MediaTypes mediaTypes;
    
    @Inject
    public ExceptionMapperBase(Optional<Ontology> ontology, Optional<TemplateCall> templateCall, MediaTypes mediaTypes)
    {
        this.ontology = ontology;
        this.templateCall = templateCall;
        this.mediaTypes = mediaTypes;
    }

    public Resource toResource(Exception ex, Response.StatusType status, Resource statusResource)
    {
        if (ex == null) throw new IllegalArgumentException("Exception cannot be null");
        if (status == null) throw new IllegalArgumentException("Response.Status cannot be null");

        Resource resource = ModelFactory.createDefaultModel().createResource().
                addProperty(RDF.type, HTTP.Response).
                addLiteral(HTTP.statusCodeValue, status.getStatusCode()).
                addLiteral(HTTP.reasonPhrase, status.getReasonPhrase());

        if (statusResource != null) resource.addProperty(HTTP.sc, statusResource);
        if (ex.getMessage() != null) resource.addLiteral(DCTerms.title, ex.getMessage());
        
        return resource;
    }
    
    public Response.ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        Variant variant = getRequest().selectVariant(getVariants(Dataset.class));
        if (variant == null) return getResponseBuilder(dataset.getDefaultModel()); // if quads are not acceptable, fallback to responding with the default graph
        
        Response.ResponseBuilder builder = new com.atomgraph.core.model.impl.Response(getRequest(),
                dataset,
                null,
                new EntityTag(Long.toHexString(com.atomgraph.core.model.impl.Response.hashDataset(dataset))),
                variant).
                getResponseBuilder().
            header(HttpHeaders.LINK, new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));
        if (getTemplateCall().isPresent()) builder.header(HttpHeaders.LINK, new Link(URI.create(getTemplateCall().get().getTemplate().getURI()), LDT.template.getURI(), null));
        if (getOntology().isPresent()) builder.header(HttpHeaders.LINK, new Link(URI.create(getOntology().get().getURI()), LDT.ontology.getURI(), null));
                
            // Jersey's Link is buggy: https://github.com/eclipse-ee4j/jersey/issues/4545
//            header(HttpHeaders.LINK, Link.fromUri(getUriInfo().getBaseUri()).rel(LDT.base.getURI()).build());
//        if (getTemplateCall().isPresent()) builder.header(HttpHeaders.LINK, Link.fromUri(getTemplateCall().get().getTemplate().getURI()).rel(LDT.template.getURI()).build());
//        if (getOntology() != null) builder.header(HttpHeaders.LINK, Link.fromUri(getOntology().getURI()).rel(LDT.ontology.getURI()).build());
        
        return builder;
    }
    
    public Response.ResponseBuilder getResponseBuilder(Model model)
    {
        Variant variant = getRequest().selectVariant(getVariants(Model.class));
        if (variant == null) variant = new Variant(com.atomgraph.core.MediaType.TEXT_TURTLE_TYPE, (Locale)null, null); // if still not acceptable, default to Turtle

        Response.ResponseBuilder builder = new com.atomgraph.core.model.impl.Response(getRequest(),
                model,
                null,
                new EntityTag(Long.toHexString(ModelUtils.hashModel(model))),
                variant).
                getResponseBuilder().
            header(HttpHeaders.LINK, new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));
        if (getTemplateCall().isPresent()) builder.header(HttpHeaders.LINK, new Link(URI.create(getTemplateCall().get().getTemplate().getURI()), LDT.template.getURI(), null));
        if (getOntology().isPresent()) builder.header(HttpHeaders.LINK, new Link(URI.create(getOntology().get().getURI()), LDT.ontology.getURI(), null));
        
            // Jersey's Link is buggy: https://github.com/eclipse-ee4j/jersey/issues/4545
//            header(HttpHeaders.LINK, Link.fromUri(getUriInfo().getBaseUri()).rel(LDT.base.getURI()).build());
//        if (getTemplateCall().isPresent()) builder.header(HttpHeaders.LINK, Link.fromUri(getTemplateCall().get().getTemplate().getURI()).rel(LDT.template.getURI()).build());
//        if (getOntology() != null) builder.header(HttpHeaders.LINK, Link.fromUri(getOntology().getURI()).rel(LDT.ontology.getURI()).build());
        
        return builder;
    }

    /**
     * Builds a list of acceptable response variants for a certain class.
     * 
     * @param clazz class
     * @return list of variants
     */
    public List<Variant> getVariants(Class clazz)
    {
        return getVariants(getWritableMediaTypes(clazz));
    }
    
    /**
     * Builds a list of acceptable response variants.
     * 
     * @param mediaTypes
     * @return supported variants
     */
    public List<Variant> getVariants(List<MediaType> mediaTypes)
    {
        return com.atomgraph.core.model.impl.Response.getVariantListBuilder(mediaTypes, getLanguages(), getEncodings()).add().build();
    }
    
    /**
     * Get writable media types for a certain class.
     * 
     * @param clazz class
     * @return list of media types
     */
    public List<MediaType> getWritableMediaTypes(Class clazz)
    {
        return getMediaTypes().getWritable(clazz);
    }
    
    public List<Locale> getLanguages()
    {
        return new ArrayList<>();
    }

    public List<String> getEncodings()
    {
        return new ArrayList<>();
    }
        
    public Request getRequest()
    {
        return request;
    }
    
    public Optional<TemplateCall> getTemplateCall()
    {
        return templateCall;
    }

    public Optional<Ontology> getOntology()
    {
        return ontology;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}