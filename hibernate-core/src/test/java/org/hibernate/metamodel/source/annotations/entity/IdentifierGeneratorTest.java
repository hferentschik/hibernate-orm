/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.Assigned;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.service.ServiceRegistryBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class IdentifierGeneratorTest extends BaseAnnotationBindingTestCase {
	@Entity
	class NoGenerationEntity {
		@Id
		private long id;
	}

	@Test
	@Resources(annotatedClasses = NoGenerationEntity.class)
	public void testNoIdGeneration() {
		EntityBinding binding = getEntityBinding( NoGenerationEntity.class );
		EntityIdentifier identifier = binding.getHierarchyDetails().getEntityIdentifier();
		IdGenerator generator = identifier.getIdGenerator();
		assertEquals( "Wrong generator", "default_assign_identity_generator", generator.getName() );
	}

	@Entity
	class AutoEntity {
		@Id
		@GeneratedValue
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = AutoEntity.class)
	public void testAutoGenerationType() {
		EntityBinding binding = getEntityBinding( AutoEntity.class );
		IdGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdGenerator();
		assertEquals( "Wrong generator", "native", generator.getStrategy() );
	}

	@Entity
	class TableEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = TableEntity.class)
	public void testTableGenerationType() {
		EntityBinding binding = getEntityBinding( TableEntity.class );
		IdGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdGenerator();
		assertEquals( "Wrong generator", "org.hibernate.id.MultipleHiLoPerTableGenerator", generator.getStrategy() );
	}

	@Entity
	class SequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = SequenceEntity.class)
	public void testSequenceGenerationType() {
		EntityBinding binding = getEntityBinding( SequenceEntity.class );
		IdGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdGenerator();
		assertEquals( "Wrong generator", "seqhilo", generator.getStrategy() );
	}


	@Entity
	class NamedGeneratorEntity {
		@Id
		@GeneratedValue(generator = "my-generator")
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	public void testUndefinedGenerator() {
		try {
			sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
			sources.addAnnotatedClass( NamedGeneratorEntity.class );
			sources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
			assertTrue( e.getMessage().startsWith( "Unable to find named generator" ) );
		}
	}

	@Entity
	@GenericGenerator(name = "my-generator", strategy = "uuid")
	class NamedGeneratorEntity2 {
		@Id
		@GeneratedValue(generator = "my-generator")
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = NamedGeneratorEntity2.class)
	public void testNamedGenerator() {
		EntityBinding binding = getEntityBinding( NamedGeneratorEntity2.class );
		IdGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdGenerator();
		assertEquals( "Wrong generator", "uuid", generator.getStrategy() );
	}
}


