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
package org.graphity.processor.exception;

import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ModelException extends RuntimeException
{

    private final Model model;

    public ModelException(Model model)
    {
        this.model = model;
    }
    
    public ModelException(String message, Model model)
    {
        super(message);
        this.model = model;
    }

    public ModelException(Throwable throwable, Model model)
    {
        super(throwable);
        this.model = model;
    }

    public ModelException(String message, Throwable throwable, Model model)
    {
        super(message, throwable);
        this.model = model;
    }

    public Model getModel()
    {
        return model;
    }

}