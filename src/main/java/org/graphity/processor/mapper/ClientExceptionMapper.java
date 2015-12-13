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

package org.graphity.processor.mapper;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.graphity.core.exception.ClientException;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ClientExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<ClientException>
{

    @Override
    public Response toResponse(ClientException ex)
    {
        return org.graphity.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(toResource(ex, Response.Status.INTERNAL_SERVER_ERROR,
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel(), getVariants()).
                status(Response.Status.INTERNAL_SERVER_ERROR).
                build();    
    }
    
}
