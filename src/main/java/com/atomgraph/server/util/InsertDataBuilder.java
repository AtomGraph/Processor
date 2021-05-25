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

package com.atomgraph.server.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

/**
 *
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public class InsertDataBuilder
{

    private final Model model;
    private String baseURI, graphURI;
    
    private InsertDataBuilder(Model model)
    {
        this.model = model;
    }
    
    public static InsertDataBuilder fromModel(Model model)
    {
        return new InsertDataBuilder(model);
    }
    
    public InsertDataBuilder base(String baseURI)
    {
        this.baseURI = baseURI;
        return this;
    }

    public InsertDataBuilder graph(String graphURI)
    {
        this.graphURI = graphURI;
        return this;
    }

    public UpdateRequest build() throws UnsupportedEncodingException, IOException
    {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            model.write(baos, Lang.NTRIPLES.getName());
            String body = "INSERT DATA {\n";
            
            if (getGraph() != null) body += "GRAPH <" + getGraph() + "> {\n";
            body += baos.toString(StandardCharsets.UTF_8.name()) + "\n";
            if (getGraph() != null) body += "}\n";
            
            body += "}";

            return UpdateFactory.create(body, getBase());
        }
    }
    
    private String getBase()
    {
        return baseURI;
    }
    
    private String getGraph()
    {
        return graphURI;
    }
    
}
