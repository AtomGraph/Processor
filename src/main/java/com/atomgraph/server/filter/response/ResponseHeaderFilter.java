/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.server.filter.response;

import com.atomgraph.core.util.Link;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.vocabulary.LDT;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.apache.jena.ontology.Ontology;

/**
 *
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public class ResponseHeaderFilter implements ContainerResponseFilter
{
    
    @Inject javax.inject.Provider<Optional<Ontology>> ontology;
    @Inject javax.inject.Provider<Optional<TemplateCall>> templateCall;
    
    @Context UriInfo uriInfo;
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException
    {
        response.getHeaders().add(HttpHeaders.LINK, new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));

        if (getOntology().isPresent()) // if it's not present, Link headers might be forwarded by ProxyResourceBase
            response.getHeaders().add(HttpHeaders.LINK, new Link(URI.create(getOntology().get().getURI()), LDT.ontology.getURI(), null));
        if (getTemplateCall().isPresent())
            response.getHeaders().add(HttpHeaders.LINK, new Link(URI.create(getTemplateCall().get().getTemplate().getURI()), LDT.template.getURI(), null));
    }

    public Optional<Ontology> getOntology()
    {
        return ontology.get();
    }
    
    public Optional<TemplateCall> getTemplateCall()
    {
        return templateCall.get();
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}
