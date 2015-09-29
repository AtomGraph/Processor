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

package org.graphity.processor.util;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.List;
import org.graphity.processor.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.system.SPINModuleRegistry;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Validator
{
    private static final Logger log = LoggerFactory.getLogger(Validator.class);
    
    private OntModel ontModel;
    
    protected Validator()
    {
    }
    
    protected static Validator newInstance()
    {
	return new Validator();
    }
    
    public Validator ontModel(OntModel ontModel)
    {
	if (ontModel == null) throw new IllegalArgumentException("OntModel cannot be null");
        this.ontModel = ontModel;
        SPINModuleRegistry.get().registerAll(ontModel, null);
        return this;
    }
    
    public static Validator fromOntModel(OntModel ontModel)
    {
        return newInstance().ontModel(ontModel);
    }

    public Model validate(Model model)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");

        // It looks like we don't need annotation inheritance Rules reasoner during validation.
        // Existing data should be valid; only the incoming RDF Model should be able to violate the constraints.
        OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM; // getOntModel().getSpecification()
        OntModel tempModel = ModelFactory.createOntologyModel(ontModelSpec);
        tempModel.add(getOntModel()).add(model);
	List<ConstraintViolation> cvs = SPINConstraints.check(tempModel, null);
	if (!cvs.isEmpty())
        {
            if (log.isDebugEnabled()) log.debug("SPIN constraint violations: {}", cvs);
            throw new ConstraintViolationException(cvs, model);
        }
        
        return model;
    }

    public OntModel getOntModel()
    {
        return ontModel;
    }

}
