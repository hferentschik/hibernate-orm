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

import org.hibernate.internal.util.Value;

/**
 * Binds a composite entity identifier.
 *
 * @author Hardy Ferentschik
 */
public class CompositeEntityIdentifier extends AbstractEntityIdentifier {
	private ComponentAttributeBinding componentAttributeBinding;

	/**
	 * Create a composite identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public CompositeEntityIdentifier(EntityBinding entityBinding) {
		super( entityBinding );
	}

	public void setAttributeBinding(ComponentAttributeBinding componentAttributeBinding) {
		this.componentAttributeBinding = componentAttributeBinding;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public ComponentAttributeBinding getValueBinding() {
		return componentAttributeBinding;
	}

	/**
	 * @return a value instance of containing the class in which the id attributes are declared
	 */
	public Value<Class<?>> getAttributeDeclaringClass() {
		return null;
	}

	public boolean isEmbedded() {
		// TODO
		return false;
	}

	public boolean isIdentifierMapper() {
		// todo
		return false;
	}
}
