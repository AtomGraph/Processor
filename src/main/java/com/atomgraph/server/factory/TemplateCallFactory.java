/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.server.factory;

import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.model.impl.TemplateCallImpl;
import com.atomgraph.processor.util.TemplateMatcher;
import java.net.URI;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template call provider.
 * 
 * @see com.atomgraph.processor.model.impl.TemplateCallImpl
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
public class TemplateCallFactory implements Factory<Optional<TemplateCall>>
{

    private static final Logger log = LoggerFactory.getLogger(TemplateCallFactory.class);

    @Context UriInfo uriInfo;
    
    @Inject Optional<Ontology> ontology;

    @Override
    public Optional<TemplateCall> provide()
    {
        return getTemplateCall();
    }

    @Override
    public void dispose(Optional<TemplateCall> tc)
    {
    }
    
    public Optional<TemplateCall> getTemplateCall()
    {
        Template template = getTemplate();
        if (template != null) return getTemplateCall(template, getUriInfo().getAbsolutePath(), getUriInfo().getQueryParameters());
        
        return Optional.empty();
    }
    
    public Optional<TemplateCall> getTemplateCall(Template template, URI absolutePath, MultivaluedMap<String, String> queryParams)
    {
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
        if (absolutePath == null) throw new IllegalArgumentException("URI cannot be null");
        if (queryParams == null) throw new IllegalArgumentException("MultivaluedMap cannot be null");

        //if (log.isDebugEnabled()) log.debug("Building Optional<TemplateCall> from Template {}", template);
        TemplateCall templateCall = new TemplateCallImpl(ModelFactory.createDefaultModel().createResource(absolutePath.toString()), template).
            applyArguments(queryParams). // apply URL query parameters
            applyDefaults().
            validateOptionals(); // validate (non-)optional arguments
        templateCall.build(); // build state URI
        
        return Optional.of(templateCall);
    }

    public Template getTemplate()
    {
        if (getOntology().isPresent()) return getTemplate(getOntology().get(), getUriInfo());
        
        return null;
    }

    public Template getTemplate(Ontology ontology, UriInfo uriInfo)
    {
        return new TemplateMatcher(ontology).match(uriInfo.getAbsolutePath(), uriInfo.getBaseUri());
    }
    
    public Optional<Ontology> getOntology()
    {
        return ontology;
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}
