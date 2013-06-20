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
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

/**
 * Represents the information about an entity annotated with {@code @Embeddable}.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableClass extends ConfiguredClass {
	private final String embeddedAttributeName;
	private final String parentReferencingAttributeName;
	//custom tuplizer defined on the embedded field
	private final String customTuplizerClass;
	private SingularAttributeBinding.NaturalIdMutability naturalIdMutability;

	/**
	 * Default constructor
	 *
	 * @param classInfo the Jandex {@code ClassInfo} for this mapped superclass
	 * @param fullyResolvedType the resolved generic type information (via classmate)
	 * @param embeddedAttributeName the name of the embedded attribute
	 * @param defaultAccessType the default access type
	 * @param parent the parent class
	 * @param customTuplizerClass custom tuplizer
	 * @param context context
	 */
	public EmbeddableClass(
			ClassInfo classInfo,
			ResolvedTypeWithMembers fullyResolvedType,
			String embeddedAttributeName,
			ConfiguredClass parent,
			AccessType defaultAccessType,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			String customTuplizerClass,
			AnnotationBindingContext context) {
		super( classInfo, fullyResolvedType, defaultAccessType, parent, context );
		this.embeddedAttributeName = embeddedAttributeName;
		this.naturalIdMutability = naturalIdMutability;
		this.parentReferencingAttributeName = checkParentAnnotation();
		this.customTuplizerClass = customTuplizerClass;
	}

	private String checkParentAnnotation() {
		final AnnotationInstance parentAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.PARENT
		);
		return parentAnnotation == null ? null : JandexHelper.getPropertyName( parentAnnotation.target() );
	}

	public String getEmbeddedAttributeName() {
		return embeddedAttributeName;
	}

	public String getParentReferencingAttributeName() {
		return parentReferencingAttributeName;
	}

	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	public void setNaturalIdMutability(SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		this.naturalIdMutability = naturalIdMutability;
	}

	public String getCustomTuplizerClass() {
		return customTuplizerClass;
	}
}


