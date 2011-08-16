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

/**
 * Binds the entity identifier.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface EntityIdentifier {
	/**
	 * @return A reference to the entity binding containing this identifier
	 */
	public EntityBinding getEntityBinding();

	/**
	 * @return {@code true} is this identifier is a simple identifier of {@code false} if it
	 *         is a composite identifier
	 */
	public boolean isSimple();

	public IdGenerator getIdGenerator();

	public SingularAttributeBinding getAttributeBinding();
}
