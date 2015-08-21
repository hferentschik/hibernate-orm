package org.hibernate.jpamodelgen.test.innerclasses;

import org.junit.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class AnnotatedInnerClassesGetProcessedTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "HHH-8714")
	@WithClasses(WorkOrder.class)
	public void verify_that_metamodel_for_embeddable_specified_as_inner_class_gets_generated() {
		assertMetamodelClassGeneratedFor( WorkOrder.WorkOrderId.class );
	}
}
