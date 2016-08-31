/**
 *  Copyright 2014 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.provider;

import org.apache.jena.query.Dataset;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ContextResolver;
import org.graphity.core.MediaTypes;
import org.graphity.server.service.GraphStoreFactory;
import org.graphity.core.model.GraphStore;
import org.graphity.core.util.jena.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for Graph Store.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see org.graphity.core.model.GraphStore
 */
public class GraphStoreProvider extends org.graphity.core.provider.GraphStoreProvider
{
    
    private static final Logger log = LoggerFactory.getLogger(GraphStoreProvider.class);

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
     * Provides a proxy if graph store origin is configured, and a local dataset-backed graphs store if it is not.
     * 
     * @return graph store instance 
     */
    @Override
    public GraphStore getGraphStore()
    {
        if (getOrigin() == null) // use local graph store
            return getGraphStore(getRequest(), getServletConfig(), getMediaTypes(), getDataset(), getDataManager());
        
        return super.getGraphStore();
   }

    public GraphStore getGraphStore(Request request, ServletConfig servletConfig, MediaTypes mediaTypes, Dataset dataset, org.graphity.core.util.jena.DataManager dataManager)
    {
        return GraphStoreFactory.create(request, servletConfig, mediaTypes, dataset, dataManager);        
    }
    
}
