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

package org.graphity.processor.provider;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import org.graphity.processor.util.Skolemizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SkolemizingModelProvider extends ValidatingModelProvider
{
    private static final Logger log = LoggerFactory.getLogger(SkolemizingModelProvider.class);
    
    @Context private Request request;
    @Context private UriInfo uriInfo;
    
    @Override
    public Model process(Model model)
    {
        model = super.process(model);
        
        if (getRequest().getMethod().equalsIgnoreCase("POST"))
        {
            try
            {
                return skolemize(getUriInfo(), getOntModel(), getOntClass(), new OntClassMatcher(), model);
            }
            catch (IllegalArgumentException ex)
            {
                if (log.isErrorEnabled()) log.error("Blank node skolemization failed for model: {}", model);
                throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
            }
        }
        
        return model;
    }
    
    public Model skolemize(UriInfo uriInfo, OntModel ontModel, OntClass ontClass, OntClassMatcher ontClassMatcher, Model model)
    {
        return Skolemizer.fromOntModel(ontModel).
                uriInfo(uriInfo).
                ontClass(ontClass).
                ontClassMatcher(ontClassMatcher).
                build(model);
    }

    public OntClass getOntClass()
    {
	ContextResolver<OntClass> cr = getProviders().getContextResolver(OntClass.class, null);
	return cr.getContext(OntClass.class);
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public Request getRequest()
    {
        return request;
    }
    
}
