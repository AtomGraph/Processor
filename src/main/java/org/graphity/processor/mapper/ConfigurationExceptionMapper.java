/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graphity.processor.mapper;

import org.apache.jena.rdf.model.ResourceFactory;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.graphity.core.exception.ConfigurationException;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ConfigurationExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<ConfigurationException>
{

    @Override
    public Response toResponse(ConfigurationException ex)
    {
        return org.graphity.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(toResource(ex, Response.Status.INTERNAL_SERVER_ERROR,
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel(), getVariants()).
                status(Response.Status.INTERNAL_SERVER_ERROR).
                build();    
    }
    
}
