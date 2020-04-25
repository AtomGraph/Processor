/*
 * Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.processor.model.impl;

import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.TemplateCall;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCallFactory
{

    public static TemplateCall fromUri(String uri, Model model, Template template)
    {
        if (uri == null) throw new IllegalArgumentException("URI String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
        return new TemplateCallImpl(model.createResource(uri), template);
    }

    public static TemplateCall fromResource(Resource resource, Template template)
    {
        return new TemplateCallImpl(resource, template);
    }

}