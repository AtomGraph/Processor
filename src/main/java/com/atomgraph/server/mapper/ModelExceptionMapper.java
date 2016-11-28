/*
 * Copyright 2013 Martynas Jusevičius <martynas@atomgraph.com>.
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

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import com.atomgraph.core.util.Link;
import com.atomgraph.processor.exception.ModelException;
import com.atomgraph.processor.vocabulary.LDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ModelExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<ModelException>
{
    private static final Logger log = LoggerFactory.getLogger(ModelExceptionMapper.class);
        
    @Override
    public Response toResponse(ModelException ex)
    {
        Resource exception = toResource(ex, Response.Status.BAD_REQUEST,
            ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#BadRequest"));
        ex.getModel().add(exception.getModel());
        
        Link classLink = new Link(URI.create(getStateBuilder().getTemplate().getURI()), RDF.type.getLocalName(), null);
        Link ontologyLink = new Link(URI.create(getOntology().getURI()), LDT.ontology.getURI(), null);
        Link baseUriLink = new Link(getUriInfo().getBaseUri(), LDT.baseUri.getURI(), null); // LDT.baseUri?
        
        ResponseBuilder builder = com.atomgraph.core.model.impl.Response.fromRequest(getRequest()).
            getResponseBuilder(ex.getModel(), getVariants()).
                status(Response.Status.BAD_REQUEST).
                header("Link", classLink.toString()).
                header("Link", ontologyLink.toString()).
                header("Link", baseUriLink.toString());

        /*
        Reasoner reasoner = getOntology().getOntModel().getSpecification().getReasoner();
        if (reasoner instanceof GenericRuleReasoner)
        {
            List<Rule> rules = ((GenericRuleReasoner)reasoner).getRules();
            builder.header("Rules", RulePrinter.print(rules));
        }
        */
        
        return builder.build();
    }
    
}
