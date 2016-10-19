/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.processor.util;

import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.vocabulary.LDT;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class StateBuilder extends com.atomgraph.core.util.StateBuilder
{
    
    protected StateBuilder(UriBuilder uriBuilder, Model model)
    {
        super(uriBuilder, model);
    }
    
    public static StateBuilder fromUri(URI uri, Model model)
    {
        return new StateBuilder(UriBuilder.fromUri(uri), model);
    }

    public static StateBuilder fromUri(String uri, Model model)
    {
        return new StateBuilder(UriBuilder.fromUri(uri), model);
    }

    public static StateBuilder fromResource(Resource resource)
    {
        return new StateBuilder(UriBuilder.fromUri(resource.getURI()), resource.getModel());
    }

    public StateBuilder apply(TemplateCall templateCall)
    {
        if (templateCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
        
        StmtIterator it = templateCall.listProperties();
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                // ignore system properties on TemplateCall. TO-DO: find a better solution to this
                if (!stmt.getPredicate().equals(RDF.type) && !stmt.getPredicate().equals(LDT.template))
                    replaceProperty(stmt.getPredicate(), stmt.getObject());
            }
        }
        finally
        {
            it.close();
        }
        
        return this;
    }

}
