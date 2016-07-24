/*
 * Copyright 2016 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.mapper;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import org.graphity.core.util.Link;
import org.graphity.core.vocabulary.G;
import org.graphity.processor.exception.SkolemizationException;
import org.graphity.processor.vocabulary.GP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SkolemizationExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<SkolemizationException>
{

    @Override
    public Response toResponse(SkolemizationException ske)
    {
        Resource exception = toResource(ske, Response.Status.BAD_REQUEST,
            ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#BadRequest"));
        ske.getModel().add(exception.getModel());
       
        Link classLink = new Link(URI.create(getMatchedOntClass().getURI()), RDF.type.getLocalName(), null);
        Link ontologyLink = new Link(URI.create(getOntology().getURI()), GP.ontology.getURI(), null);
        Link baseUriLink = new Link(getUriInfo().getBaseUri(), G.baseUri.getURI(), null);
        
        Variant variant = getVariant();
        org.graphity.core.model.impl.Response response = org.graphity.core.model.impl.Response.fromRequest(getRequest());
        return response.getResponseBuilder(ske, response.getEntityTag(ske.getModel(), variant), variant).
                status(Response.Status.BAD_REQUEST).
                header("Link", classLink.toString()).
                header("Link", ontologyLink.toString()).
                header("Link", baseUriLink.toString()).                
                build();
    }
    
}
