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
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Property;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.net.URI;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.apache.jena.riot.RDFDataMgr;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.processor.vocabulary.AP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for dataset.
 * Needs to be registered in the application.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see com.hp.hpl.jena.query.Dataset 
 */
@Provider
public class DatasetProvider extends PerRequestTypeInjectableProvider<Context, Dataset> implements ContextResolver<Dataset>
{
    private static final Logger log = LoggerFactory.getLogger(DatasetProvider.class);

    @Context UriInfo uriInfo;
    @Context ServletConfig servletConfig;

    public DatasetProvider()
    {
        super(Dataset.class);
    }

    @Override
    public Injectable<Dataset> getInjectable(ComponentContext cc, Context a)
    {
	return new Injectable<Dataset>()
	{
	    @Override
	    public Dataset getValue()
	    {
		return getDataset();
	    }
	};
    }
    
    /**
     * Returns configured dataset instance.
     * Uses <code>gp:dataset</code> context parameter value from web.xml as dataset location.
     * 
     * @return dataset instance
     */
    public Dataset getDataset()
    {
        String datasetLocation = getDatasetLocation(AP.dataset);
        if (datasetLocation == null)
        {
            if (log.isErrorEnabled()) log.error("Application dataset ({}) is not configured in web.xml", AP.dataset.getURI());
            throw new ConfigurationException("Application dataset (" + AP.dataset.getURI() + ") is not configured in web.xml");
        }

        return getDataset(datasetLocation, getUriInfo().getBaseUri());
    }
    
    public String getDatasetLocation(Property property)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object datasetLocation = getServletConfig().getInitParameter(property.getURI());
        if (datasetLocation != null) return datasetLocation.toString();
        
        return null;
    }
    
    public Dataset getDataset(String location, URI baseURI)
    {
        if (location == null) throw new IllegalArgumentException("Location String cannot be null");
        if (baseURI == null) throw new IllegalArgumentException("Base URI cannot be null");
	
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, location, baseURI.toString(), null); // Lang.TURTLE
        return dataset;
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    @Override
    public Dataset getContext(Class<?> type)
    {
        return getDataset();
    }
    
}