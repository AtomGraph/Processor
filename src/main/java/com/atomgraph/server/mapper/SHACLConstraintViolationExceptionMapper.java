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
package com.atomgraph.server.mapper;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.server.exception.SHACLConstraintViolationException;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SHACLConstraintViolationExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<SHACLConstraintViolationException>
{

    @Inject
    public SHACLConstraintViolationExceptionMapper(MediaTypes mediaTypes)
    {
        super(mediaTypes);
    }

    @Override
    public Response toResponse(SHACLConstraintViolationException ex)
    {
        Resource exception = toResource(ex, Response.Status.BAD_REQUEST,
            ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#BadRequest"));
        ex.getModel().add(exception.getModel());
        
//        SPINConstraints.addConstraintViolationsRDF(ex.getConstraintViolations(), ex.getModel(), true);
//        ResIterator it = ex.getModel().listSubjectsWithProperty(RDF.type, SPIN.ConstraintViolation);
//        try
//        {
//            while (it.hasNext())
//            {
//                Resource violation = it.next();
//                // connect Response to ConstraintViolations
//                ex.getModel().add(exception, ResourceFactory.createProperty("http://www.w3.org/ns/prov#wasDerivedFrom"), violation);
//            }
//        }
//        finally
//        {
//            it.close();
//        }
        ex.getModel().add(ex.getValidationReport().getModel());

        return getResponseBuilder(ex.getModel()).
                status(Response.Status.BAD_REQUEST).
                build();
    }
    
}
