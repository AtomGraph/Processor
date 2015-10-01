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

package org.graphity.processor;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{

    @Context Providers providers;
    @Context UriInfo uriInfo;
    @Context ServletConfig servletConfig;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (response.getEntity() != null && response.getEntity() instanceof Model)
        {
            Model model = (Model)response.getEntity();
            Resource resource = model.createResource(request.getAbsolutePath().toString());
            //new HypermediaBase().
        }
        
        return response;
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
    
    public OntClass getMatchedOntClass()
    {
	return getProviders().getContextResolver(OntClass.class, null).getContext(OntClass.class);
    }

    public Ontology getOntology()
    {
	return getProviders().getContextResolver(Ontology.class, null).getContext(Ontology.class);
    }
    
}
