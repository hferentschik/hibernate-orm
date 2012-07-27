/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tool.hbm2ddl.Exporter;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.Target;
import org.hibernate.validator.constraints.Length;

import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */

public class ApplySchemaConstraintTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testLengthConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry() );
		SchemaExport schemaExport = new SchemaExport( metadata );
	}

	@Test
	public void testLengthConstraintAppliedWithConfiguration() throws Exception {
		SchemaExport schemaExport = new SchemaExport( serviceRegistry(), configuration() );
		BufferingExporter bufferingExporter = new BufferingExporter();
		schemaExport.setCustomExporter( bufferingExporter );
		schemaExport.create( Target.SCRIPT );
		assertTrue(bufferingExporter.getExportScript().contains( "s varchar(10)" ));
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class,
		};
	}

	@Entity
	public static class Foo {
		@Id
		@GeneratedValue
		private int id;

		@Length(max = 10)
		private String s;

	}

	public static class BufferingExporter implements Exporter {
		StringBuilder builder = new StringBuilder();

		@Override
		public boolean acceptsImportScripts() {
			return false;
		}

		@Override
		public void export(String string) throws Exception {
			builder.append( string );
		}

		@Override
		public void release() throws Exception {
		}

		public String getExportScript() {
			return builder.toString();
		}
	}
}
