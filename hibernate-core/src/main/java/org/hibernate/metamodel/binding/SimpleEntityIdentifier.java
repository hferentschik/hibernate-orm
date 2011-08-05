/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;

/**
 * Binds the entity identifier.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class SimpleEntityIdentifier implements EntityIdentifier {
	private final EntityBinding entityBinding;
	private BasicAttributeBinding attributeBinding;
	private IdentifierGenerator identifierGenerator;
	private IdGenerator idGenerator;
	private boolean isIdentifierMapper = false;

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public SimpleEntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public BasicAttributeBinding getValueBinding() {
		return attributeBinding;
	}

	public void setValueBinding(BasicAttributeBinding attributeBinding) {
		if ( this.attributeBinding != null ) {
			throw new AssertionFailure(
					String.format(
							"Identifier value binding already existed for %s",
							entityBinding.getEntity().getName()
					)
			);
		}
		this.attributeBinding = attributeBinding;
	}

	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	public boolean isEmbedded() {
		return attributeBinding.getSimpleValueSpan() > 1;
	}

	public boolean isIdentifierMapper() {
		return isIdentifierMapper;
	}

	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		if ( idGenerator != null ) {
			identifierGenerator = attributeBinding.createIdentifierGenerator( idGenerator, factory, properties );
		}
		return identifierGenerator;
	}

	@Override
	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	@Override
	public boolean isSimple() {
		return true;
	}

	@Override
	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}
}
