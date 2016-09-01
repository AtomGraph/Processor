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

package com.atomgraph.processor.exception;

import org.apache.jena.rdf.model.Model;
import java.util.List;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ConstraintViolationException extends ModelException
{
    private final List<ConstraintViolation> cvs;
    
    public ConstraintViolationException(List<ConstraintViolation> cvs, Model model)
    {
        super(model);
	this.cvs = cvs;
        SPINConstraints.addConstraintViolationsRDF(cvs, model, true);
    }

    public List<ConstraintViolation> getConstraintViolations()
    {
	return cvs;
    }
    
}
