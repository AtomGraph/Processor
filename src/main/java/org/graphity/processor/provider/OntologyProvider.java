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
package org.graphity.processor.provider;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.naming.ConfigurationException;
import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider for ontology model.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see com.hp.hpl.jena.ontology.OntModel
 */
@Provider
public class OntologyProvider extends PerRequestTypeInjectableProvider<Context, Ontology> implements ContextResolver<Ontology>
{
    private static final Logger log = LoggerFactory.getLogger(OntologyProvider.class);

    @Context UriInfo uriInfo;
    @Context Request request;
    @Context ServletConfig servletConfig;
    @Context Providers providers;

    public OntologyProvider()
    {
	super(Ontology.class);
    }

    public ServletConfig getServletConfig()
    {
	return servletConfig;
    }

    public UriInfo getUriInfo()
    {
	return uriInfo;
    }

    public Request getRequest()
    {
        return request;
    }

    @Override
    public Injectable<Ontology> getInjectable(ComponentContext cc, Context context)
    {
	//if (log.isDebugEnabled()) log.debug("OntologyProvider UriInfo: {} ResourceConfig.getProperties(): {}", uriInfo, resourceConfig.getProperties());
	
	return new Injectable<Ontology>()
	{
	    @Override
	    public Ontology getValue()
	    {
                return getOntology();
	    }
	};
    }

    @Override
    public Ontology getContext(Class<?> type)
    {
        return getOntology();
    }

    public Ontology getOntology()
    {
        try
        {
            Ontology ontology = getOntology(getServletConfig(), GP.sitemap);
            if (ontology == null)
            {
                if (log.isErrorEnabled()) log.error("Sitemap ontology resource not found; processing aborted");
                throw new ConfigurationException("Sitemap ontology resource not found; processing aborted");
            }
            return ontology;
        }
        catch (ConfigurationException ex)
        {
            throw new WebApplicationException(ex);
        }
    }
    
    /**
     * Returns configured sitemap ontology.
     * Uses <code>gp:sitemap</code> context parameter value from web.xml as dataset location.
     * 
     * @param ontModel
     * @param ontologyURI
     * @return ontology model
     */
    public Ontology getOntology(OntModel ontModel, String ontologyURI)
    {
        if (ontModel == null) throw new IllegalArgumentException("OntModel cannot be null");
        if (ontologyURI == null) throw new IllegalArgumentException("Ontology URI String cannot be null");
        
        return ontModel.getOntology(ontologyURI);
    }
            
    public Ontology getOntology(ServletConfig servletConfig, ObjectProperty property) throws ConfigurationException
    {
        String ontologyURI = getOntologyURI(servletConfig, property);
        if (ontologyURI == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology URI (gp:sitemap) not configured");
            throw new ConfigurationException("Sitemap ontology URI (gp:sitemap) not configured");
        }

        return getOntology(getOntModel(ontologyURI), ontologyURI);
    }
    
    public String getOntologyURI(ServletConfig servletConfig, ObjectProperty property)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object ontology = servletConfig.getInitParameter(property.getURI());
        if (ontology != null) return ontology.toString();

        return null;
    }
    
    /**
     * Reads ontology model from a file.
     * 
     * @param ontologyURI ontology location
     * @return ontology model
     */
    public OntModel getOntModel(String ontologyURI)
    {
        if (ontologyURI == null) throw new IllegalArgumentException("URI cannot be null");

        if (log.isDebugEnabled()) log.debug("Loading sitemap ontology from URI {}", ontologyURI);
        return OntDocumentManager.getInstance().getOntology(ontologyURI, OntModelSpec.OWL_MEM);
    }

    public Providers getProviders()
    {
        return providers;
    }

}