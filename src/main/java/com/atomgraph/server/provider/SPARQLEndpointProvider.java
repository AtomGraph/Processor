/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.atomgraph.server.provider;

import org.apache.jena.query.Dataset;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.server.service.SPARQLEndpointFactory;
import com.atomgraph.core.model.SPARQLEndpoint;
import com.atomgraph.core.util.jena.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for SPARQL endpoint.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.atomgraph.core.model.SPARQLEndpoint
 */
public class SPARQLEndpointProvider extends com.atomgraph.core.provider.SPARQLEndpointProvider
{
    
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointProvider.class);

    public Dataset getDataset()
    {
	ContextResolver<Dataset> cr = getProviders().getContextResolver(Dataset.class, null);
	return cr.getContext(Dataset.class);
    }

    public DataManager getDataManager()
    {
	ContextResolver<DataManager> cr = getProviders().getContextResolver(DataManager.class, null);
	return cr.getContext(DataManager.class);
    }
    
    /**
     * This subclass provides a proxy if endpoint origin is configured, and a local dataset-backed endpoint if it is not.
     * 
     * @return endpoint instance
     */
    @Override
    public SPARQLEndpoint getSPARQLEndpoint()
    {
        if (getOrigin() == null) // use local endpoint
            return getSPARQLEndpoint(getRequest(), getServletConfig(), getMediaTypes(), getDataset(), getDataManager());

        return super.getSPARQLEndpoint();
    }
    
    public SPARQLEndpoint getSPARQLEndpoint(Request request, ServletConfig servletConfig, MediaTypes mediaTypes, Dataset dataset, com.atomgraph.core.util.jena.DataManager dataManager)
    {
        return SPARQLEndpointFactory.create(request, servletConfig, mediaTypes, dataset, dataManager);
    }

}
