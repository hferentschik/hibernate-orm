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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.jboss.jandex.ClassInfo;

/**
 * Represents the information about an entity annotated with {@code @MappedSuperclass}.
 *
 * @author Hardy Ferentschik
 */
public class MappedSuperclass extends ConfiguredClass {
	/**
	 * Default constructor
	 *
	 * @param classInfo the Jandex {@code ClassInfo} for this mapped superclass
	 * @param fullyResolvedType the resolved generic type information (via classmate)
	 * @param parent the parent class
	 * @param defaultAccessType the default access type
	 * @param context context
	 */
	public MappedSuperclass(
			ClassInfo classInfo,
			ResolvedTypeWithMembers fullyResolvedType,
			ConfiguredClass parent,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		super( classInfo, fullyResolvedType, defaultAccessType, parent, context );
	}
}


