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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedMember;
import org.hibernate.metamodel.internal.source.annotations.util.EmbeddableHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.MappingException;

/**
 * Base class for a configured entity, mapped super class or embeddable
 *
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class ConfiguredClass {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, AssertionFailure.class.getName() );

	/**
	 * The parent of this configured class or {@code null} in case this configured class is the root of a hierarchy.
	 */
	private final ConfiguredClass parent;

	/**
	 * The Jandex class info for this configured class. Provides access to the annotation defined on this configured class.
	 */
	private final ClassInfo classInfo;

	/**
	 * The actual java type.
	 */
	private final Class<?> clazz;

	/**
	 * The generically resolved type
	 */
	private final ResolvedTypeWithMembers genericResolvedType;

	/**
	 * Is this class abstract?
	 */
	private final boolean isAbstract;

	/**
	 * The default access type for this entity
	 */
	private final AccessType classAccessType;

	/**
	 * The id attributes
	 */
	private final Map<String, MappedAttribute> idAttributeMap;

	/**
	 * The mapped association attributes for this entity
	 */
	private final Map<String, AssociationAttribute> associationAttributeMap;

	/**
	 * The mapped simple attributes for this entity
	 */
	private final Map<String, BasicAttribute> simpleAttributeMap;

	/**
	 * The version attribute or {@code null} in case none exists.
	 */
	private BasicAttribute versionAttribute;

	/**
	 * The embedded classes for this entity
	 */
	private final Map<String, EmbeddableClass> embeddedClasses;

	/**
	 * The collection element embedded classes for this entity
	 */
	private final Map<String, EmbeddableClass> collectionEmbeddedClasses;

	/**
	 * A map of all attribute overrides defined in this class. The override name is "normalised", meaning as if specified
	 * on class level. If the override is specified on attribute level the attribute name is used as prefix.
	 */
	private final Map<String, AttributeOverride> attributeOverrideMap;

	private final Set<String> transientFieldNames = new HashSet<String>();
	private final Set<String> transientMethodNames = new HashSet<String>();

	/**
	 * Fully qualified name of a custom tuplizer
	 */
	private final String customTuplizer;

	private final EntityBindingContext localBindingContext;

	/**
	 * Default constructor
	 *
	 * @param classInfo the Jandex {@code ClassInfo} for this mapped superclass
	 * @param fullyResolvedType the resolved generic type information (via classmate)
	 * @param defaultAccessType the default access type
	 * @param parent the parent class
	 * @param context context
	 */
	public ConfiguredClass(
			ClassInfo classInfo,
			ResolvedTypeWithMembers fullyResolvedType,
			AccessType defaultAccessType,
			ConfiguredClass parent,
			AnnotationBindingContext context) {
		this.parent = parent;
		this.classInfo = classInfo;
		this.clazz = context.locateClassByName( classInfo.toString() );
		this.genericResolvedType = fullyResolvedType;
		this.isAbstract = ReflectHelper.isAbstractClass( this.clazz );
		this.classAccessType = determineClassAccessType( defaultAccessType );
		this.customTuplizer = determineCustomTuplizer();

		this.simpleAttributeMap = new TreeMap<String, BasicAttribute>();
		this.idAttributeMap = new TreeMap<String, MappedAttribute>();
		this.associationAttributeMap = new TreeMap<String, AssociationAttribute>();
		this.embeddedClasses = new HashMap<String, EmbeddableClass>(  );
		this.collectionEmbeddedClasses = new HashMap<String, EmbeddableClass>(  );

		this.localBindingContext = new EntityBindingContext( context, this );

		collectAttributes();
		attributeOverrideMap = Collections.unmodifiableMap( findAttributeOverrides() );
	}

	public String getName() {
		return clazz.getName();
	}

	public Class<?> getConfiguredClass() {
		return clazz;
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	public ConfiguredClass getParent() {
		return parent;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public EntityBindingContext getLocalBindingContext() {
		return localBindingContext;
	}

	public boolean hostsAnnotation(DotName annotationName) {
		final List<AnnotationInstance> annotationList = classInfo.annotations().get( annotationName );
		return CollectionHelper.isNotEmpty( annotationList );
	}

	public Collection<BasicAttribute> getSimpleAttributes() {
		return simpleAttributeMap.values();
	}

	public boolean isIdAttribute(String attributeName) {
		return idAttributeMap.containsKey( attributeName );
	}

	public Collection<MappedAttribute> getIdAttributes() {
		return idAttributeMap.values();
	}

	public BasicAttribute getVersionAttribute() {
		return versionAttribute;
	}

	public Iterable<AssociationAttribute> getAssociationAttributes() {
		return associationAttributeMap.values();
	}

	public Map<String, EmbeddableClass> getEmbeddedClasses() {
		return embeddedClasses;
	}

	public Map<String, EmbeddableClass> getCollectionEmbeddedClasses() {
		return collectionEmbeddedClasses;
	}

	public Map<String, AttributeOverride> getAttributeOverrideMap() {
		return attributeOverrideMap;
	}

	public AccessType getClassAccessType() {
		return classAccessType;
	}

	public String getCustomTuplizer() {
		return customTuplizer;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{clazz=" ).append( clazz.getSimpleName() );
		sb.append( '}' );
		return sb.toString();
	}

	private AccessType determineClassAccessType(AccessType defaultAccessType) {
		// default to the hierarchy access type to start with
		AccessType accessType = defaultAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS, ClassInfo.class );
		if ( accessAnnotation != null ) {
			accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class );
		} else {
			accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, HibernateDotNames.ACCESS_TYPE, ClassInfo.class );
			if ( accessAnnotation != null ) {
				accessType = AccessType.valueOf( accessAnnotation.value().asString().toUpperCase() );
			}
		}

		return accessType;
	}

	/**
	 * Find all attributes for this configured class and add them to the corresponding map
	 */
	private void collectAttributes() {
		// find transient field and method names
		findTransientFieldAndMethodNames();

		final Set<String> explicitlyConfiguredMemberNames = createExplicitlyConfiguredAccessProperties( );

		if ( AccessType.FIELD.equals( classAccessType ) ) {
			final Field[] fields = clazz.getDeclaredFields();
			Field.setAccessible( fields, true );
			for ( Field field : fields ) {
				if ( isPersistentMember( transientFieldNames, explicitlyConfiguredMemberNames, field ) ) {
					createMappedAttribute( field, AccessType.FIELD );
				}
			}
		}
		else {
			final Method[] methods = clazz.getDeclaredMethods();
			Method.setAccessible( methods, true );
			for ( Method method : methods ) {
				if ( isPersistentMember( transientMethodNames, explicitlyConfiguredMemberNames, method ) ) {
					createMappedAttribute( method, AccessType.PROPERTY );
				}
			}
		}
	}

	private boolean isPersistentMember(Set<String> transientNames, Set<String> explicitlyConfiguredMemberNames, Member member) {
		if ( !ReflectHelper.isProperty( member ) ) {
			return false;
		}

		if ( member instanceof Field && Modifier.isStatic( member.getModifiers() ) ) {
			// static fields are no instance variables! Catches also the case of serialVersionUID
			return false;
		}

		if ( member instanceof Method && Method.class.cast( member ).getReturnType().equals( void.class ) ){
			// not a getter
			return false;
		}

		if ( transientNames.contains( member.getName() ) ) {
			return false;
		}

		return !explicitlyConfiguredMemberNames.contains( ReflectHelper.getPropertyName( member ) );

	}

	/**
	 * Creates {@code MappedProperty} instances for the explicitly configured persistent properties
	 *
	 * @return the property names of the explicitly configured attribute names in a set
	 */
	private Set<String> createExplicitlyConfiguredAccessProperties() {
		final Set<String> explicitAccessPropertyNames = new HashSet<String>();

		List<AnnotationInstance> accessAnnotations = classInfo.annotations().get( JPADotNames.ACCESS );
		final List<AnnotationInstance> hibernateAccessAnnotations = classInfo.annotations().get( HibernateDotNames.ACCESS_TYPE );
		if ( accessAnnotations == null ) {
			accessAnnotations = hibernateAccessAnnotations;
			if ( accessAnnotations == null ) {
				return explicitAccessPropertyNames;
			}
		}
		else if ( hibernateAccessAnnotations != null ) {
			accessAnnotations.addAll( hibernateAccessAnnotations );
		}

		// iterate over all @Access annotations defined on the current class
		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// we are only interested at annotations defined on fields and methods
			final AnnotationTarget annotationTarget = accessAnnotation.target();
			if ( !( annotationTarget.getClass().equals( MethodInfo.class ) || annotationTarget.getClass()
					.equals( FieldInfo.class ) ) ) {
				continue;
			}

			AccessType accessType;
			if ( JPADotNames.ACCESS.equals( accessAnnotation.name() ) ) {
				accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class );
				checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly( annotationTarget, accessType );
			}
			else {
				accessType = AccessType.valueOf( accessAnnotation.value().asString().toUpperCase() );
			}

			// the placement is correct, get the member
			Member member;
			if ( annotationTarget instanceof MethodInfo ) {
				try {
					member = clazz.getMethod( ( (MethodInfo) annotationTarget ).name() );
				}
				catch ( NoSuchMethodException e ) {
					throw new HibernateException(
							"Unable to load method "
									+ ( (MethodInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
			}
			else {
				try {
					member = clazz.getField( ( (FieldInfo) annotationTarget ).name() );
				}
				catch ( NoSuchFieldException e ) {
					throw new HibernateException(
							"Unable to load field "
									+ ( (FieldInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
			}
			if ( ReflectHelper.isProperty( member ) ) {
				createMappedAttribute( member, accessType );
				explicitAccessPropertyNames.add( ReflectHelper.getPropertyName( member ) );
			}
		}
		return explicitAccessPropertyNames;
	}

	private void checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly(AnnotationTarget annotationTarget, AccessType accessType) {
		// when the access type of the class is FIELD
		// overriding access annotations must be placed on properties AND have the access type PROPERTY
		if ( AccessType.FIELD.equals( classAccessType ) ) {
			if ( !MethodInfo.class.isInstance( annotationTarget ) ) {
				final String msg = LOG.accessTypeOverrideShouldBeAnnotatedOnProperty( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}

			if ( !AccessType.PROPERTY.equals( accessType ) ) {
				final String msg = LOG.accessTypeOverrideShouldBeProperty( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}
		}

		// when the access type of the class is PROPERTY
		// overriding access annotations must be placed on fields and have the access type FIELD
		if ( AccessType.PROPERTY.equals( classAccessType ) ) {
			if ( !FieldInfo.class.isInstance( annotationTarget ) ) {
				final String msg = LOG.accessTypeOverrideShouldBeAnnotatedOnField( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}

			if ( !AccessType.FIELD.equals( accessType ) ) {
				final String msg = LOG.accessTypeOverrideShouldBeField( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}
		}
	}

	private void createMappedAttribute(Member member, AccessType accessType) {
		final String attributeName = ReflectHelper.getPropertyName( member );
		final ResolvedMember[] resolvedMembers = Field.class.isInstance( member ) ?
				genericResolvedType.getMemberFields()
				: genericResolvedType.getMemberMethods();
		final ResolvedMember resolvedMember = findResolvedMember( member.getName(), resolvedMembers );
		final Map<DotName, List<AnnotationInstance>> annotations = JandexHelper.getMemberAnnotations(
				classInfo, member.getName(), localBindingContext.getServiceRegistry()
		);
		Class<?> attributeType = resolvedMember.getType().getErasedType();
		final Class<?> referencedCollectionType = resolveCollectionValuedReferenceType( resolvedMember, annotations );
		Class<?> indexType = null;
		if(Map.class.isAssignableFrom( attributeType )){
			indexType = resolvedMember.getType().getTypeParameters().get( 0 ).getErasedType();
		}
		final MappedAttribute.Nature attributeNature = determineAttributeNature(
				annotations, attributeType, referencedCollectionType
		);
		final String accessTypeString = accessType.toString().toLowerCase();
		switch ( attributeNature ) {
			case BASIC: {
				final BasicAttribute attribute = BasicAttribute.createSimpleAttribute(
						attributeName,
						attributeType,
						attributeNature,
						annotations,
						accessTypeString,
						getLocalBindingContext()
				);
				if ( attribute.isId() ) {
					idAttributeMap.put( attributeName, attribute );
				}
				else if ( attribute.isVersioned() ) {
					if ( versionAttribute == null ) {
						versionAttribute = attribute;
					}
					else {
						throw new MappingException( "Multiple version attributes", localBindingContext.getOrigin() );
					}
				}
				else {
					simpleAttributeMap.put( attributeName, attribute );
				}
				break;
			}
			case EMBEDDED_ID: {
				final BasicAttribute attribute = BasicAttribute.createSimpleAttribute(
						attributeName,
						attributeType,
						attributeNature,
						annotations,
						accessTypeString,
						getLocalBindingContext()
				);
				idAttributeMap.put( attributeName, attribute );
				break;
			}
			case EMBEDDED: {
				final AnnotationInstance targetAnnotation = JandexHelper.getSingleAnnotation(
						getClassInfo(),
						HibernateDotNames.TARGET
				);
				if ( targetAnnotation != null ) {
					attributeType = localBindingContext.locateClassByName(
							JandexHelper.getValue( targetAnnotation, "value", String.class )
					);
				}
				embeddedClasses.put( attributeName, resolveEmbeddable( attributeName, attributeType, resolvedMember.getType(), annotations ) );
				break;
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				final AssociationAttribute attribute = SingularAssociationAttribute.createSingularAssociationAttribute(
						classInfo,
						attributeName,
						resolvedMember.getType().getErasedType(),
						attributeNature,
						accessTypeString,
						annotations,
						getLocalBindingContext()
				);
				if(attribute.isId()){
					idAttributeMap.put( attributeName, attribute );
				}
				associationAttributeMap.put( attributeName, attribute );
				break;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE:
				final EmbeddableClass embeddableClass = resolveEmbeddable(
						attributeName,
						referencedCollectionType,
						resolvedMember.getType().getTypeBindings().getBoundType( 0 ),
						annotations
				);
				collectionEmbeddedClasses.put( attributeName, embeddableClass );
				break;
			case ELEMENT_COLLECTION_BASIC:
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				final AssociationAttribute attribute = PluralAssociationAttribute.createPluralAssociationAttribute(
						classInfo,
						attributeName,
						resolvedMember.getType().getErasedType(),
						indexType,
						referencedCollectionType,
						attributeNature,
						accessTypeString,
						annotations,
						getLocalBindingContext()
				);
				associationAttributeMap.put( attributeName, attribute );
				break;
			}
			case MANY_TO_ANY: {}
		}
	}

	private EmbeddableClass resolveEmbeddable(String attributeName,
			Class<?> type,
			ResolvedType resolvedType,
			Map<DotName, List<AnnotationInstance>> annotations) {

		final ClassInfo embeddableClassInfo = localBindingContext.getClassInfo( type.getName() );
		if ( embeddableClassInfo == null ) {
			final String msg = String.format(
					"Attribute '%s#%s' is annotated with @Embedded, but '%s' does not seem to be annotated " +
							"with @Embeddable.\n Are all annotated classes added to the configuration?",
					getConfiguredClass().getName(),
					attributeName,
					type.getName()
			);
			throw new AnnotationException( msg );
		}

		final AnnotationInstance naturalIdAnnotationInstance = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.NATURAL_ID
		);
		SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
		if ( naturalIdAnnotationInstance != null ) {
			naturalIdMutability = JandexHelper.getValue(
					naturalIdAnnotationInstance,
					"mutable",
					Boolean.class
			) ? SingularAttributeBinding.NaturalIdMutability.MUTABLE : SingularAttributeBinding.NaturalIdMutability.IMMUTABLE;
		}
		else {
			naturalIdMutability = SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
		}

		//tuplizer on field
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				annotations, HibernateDotNames.TUPLIZERS
		);
		final AnnotationInstance tuplizerAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.TUPLIZER
		);
		final String customTuplizerClass = AnnotationParserHelper.determineCustomTuplizer(
				tuplizersAnnotation,
				tuplizerAnnotation
		);

		final EmbeddableHierarchy hierarchy = EmbeddableHierarchy.createEmbeddableHierarchy(
				localBindingContext.<Object>locateClassByName( embeddableClassInfo.toString() ),
				attributeName,
				resolvedType,
				classAccessType,
				naturalIdMutability,
				customTuplizerClass,
				localBindingContext
		);
		return hierarchy.getLeaf();
	}

	/**
	 * Given the annotations defined on a persistent attribute this methods determines the attribute type.
	 *
	 * @param annotations the annotations defined on the persistent attribute
	 * @param attributeType the attribute's type
	 * @param referencedCollectionType the type of the collection element in case the attribute is collection valued
	 *
	 * @return an instance of the {@code AttributeType} enum
	 */
	private MappedAttribute.Nature determineAttributeNature(
			final Map<DotName,List<AnnotationInstance>> annotations,
			final Class<?> attributeType,
			final Class<?> referencedCollectionType ) {
		final EnumSet<MappedAttribute.Nature>  discoveredAttributeTypes = EnumSet.noneOf( MappedAttribute.Nature.class );
		final AnnotationInstance oneToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_ONE );
		if ( oneToOne != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.ONE_TO_ONE );
		}

		final AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.ONE_TO_MANY );
		}

		final AnnotationInstance manyToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_ONE );
		if ( manyToOne != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.MANY_TO_ONE );
		}

		final AnnotationInstance manyToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.MANY_TO_MANY );
		}

		final AnnotationInstance embeddedId = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED_ID );
		if ( embeddedId != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.EMBEDDED_ID );
		}
		final AnnotationInstance id = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ID );
		final AnnotationInstance embedded = JandexHelper.getSingleAnnotation(
				annotations, JPADotNames.EMBEDDED );
		if ( embedded != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.EMBEDDED );
		}
		else if ( embeddedId == null ) {
			// For backward compatibility, we're allowing attributes of an
			// @Embeddable type to leave off @Embedded.  Check the type's
			// annotations.  (see HHH-7678)
			// However, it's important to ignore this if the field is
			// annotated with @EmbeddedId.
			if ( isEmbeddableType( attributeType ) ) {
				LOG.warn( attributeType.getName() + " has @Embeddable on it, but the attribute of this type in entity["
						+ getName()
						+ "] doesn't have @Embedded, which may cause compatibility issue" );
				discoveredAttributeTypes.add( id!=null? MappedAttribute.Nature.EMBEDDED_ID :   MappedAttribute.Nature.EMBEDDED );
			}
		}

		final AnnotationInstance elementCollection = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.ELEMENT_COLLECTION
		);
		if ( elementCollection != null || ( discoveredAttributeTypes.isEmpty() && CollectionHelper.isCollectionOrArray( attributeType ) )) {
			final boolean isEmbeddable = isEmbeddableType( referencedCollectionType );
			discoveredAttributeTypes.add( isEmbeddable? MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE : MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC );
		}

		final int size = discoveredAttributeTypes.size();
		switch ( size ) {
			case 0:
				return MappedAttribute.Nature.BASIC;
			case 1:
				return discoveredAttributeTypes.iterator().next();
			default:
				throw new AnnotationException( "More than one association type configured for property  " + getName() + " of class " + getName() );
		}
	}

	protected boolean isEmbeddableType(Class<?> referencedCollectionType) {
		// class info can be null for types like string, etc where there are no annotations
		final ClassInfo classInfo = getLocalBindingContext().getIndex().getClassByName(
				DotName.createSimple(
						referencedCollectionType.getName()
				)
		);
		return classInfo != null && classInfo.annotations().get( JPADotNames.EMBEDDABLE ) != null;
	}

	private ResolvedMember findResolvedMember(String name, ResolvedMember[] resolvedMembers) {
		for ( ResolvedMember resolvedMember : resolvedMembers ) {
			if ( resolvedMember.getName().equals( name ) ) {
				return resolvedMember;
			}
		}
		throw new AssertionFailure(
				String.format(
						"Unable to resolve type of attribute %s of class %s",
						name,
						classInfo.name().toString()
				)
		);
	}

	private Class<?> resolveCollectionValuedReferenceType(
			ResolvedMember resolvedMember, Map<DotName,
			List<AnnotationInstance>> annotations) {
		final AnnotationInstance annotation;
		final String targetElementName;
		if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
			targetElementName = "targetEntity";
		}
		else if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
			targetElementName = "targetEntity";
		}
		else if ( JandexHelper.containsSingleAnnotation( annotations, JPADotNames.ELEMENT_COLLECTION ) ) {
			annotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ELEMENT_COLLECTION );
			targetElementName = "targetClass";
		}
		else {
			annotation = null;
			targetElementName = null;
		}
		if ( annotation != null && annotation.value( targetElementName ) != null ) {
			return getLocalBindingContext().locateClassByName(
					JandexHelper.getValue( annotation, targetElementName, String.class  )
			);
		}
		if ( resolvedMember.getType().isArray() ) {
			return resolvedMember.getType().getArrayElementType().getErasedType();
		}
		if ( resolvedMember.getType().getTypeParameters().isEmpty() ) {
			// no generic at all
			return null;
		}
		final Class<?> type = resolvedMember.getType().getErasedType();
		if ( Collection.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 0 ).getErasedType();
		}
		else if ( Map.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 1 ).getErasedType();
		}
		else {
			return null;
		}
	}

	/**
	 * Populates the sets of transient field and method names.
	 */
	private void findTransientFieldAndMethodNames() {
		final List<AnnotationInstance> transientMembers = classInfo.annotations().get( JPADotNames.TRANSIENT );
		if ( transientMembers == null ) {
			return;
		}

		for ( AnnotationInstance transientMember : transientMembers ) {
			final AnnotationTarget target = transientMember.target();
			if ( target instanceof FieldInfo ) {
				transientFieldNames.add( ( (FieldInfo) target ).name() );
			}
			else {
				transientMethodNames.add( ( (MethodInfo) target ).name() );
			}
		}
	}

	private Map<String, AttributeOverride> findAttributeOverrides() {
		final Map<String, AttributeOverride> attributeOverrideList
				= new HashMap<String, AttributeOverride>();

		// Add all instances of @AttributeOverride
		final List<AnnotationInstance> attributeOverrideAnnotations = JandexHelper
				.getAnnotations( classInfo, JPADotNames.ATTRIBUTE_OVERRIDE );
		if ( attributeOverrideAnnotations != null ) {
			for ( AnnotationInstance annotation : attributeOverrideAnnotations ) {
				final AttributeOverride override = new AttributeOverride(
						createPathPrefix( annotation.target() ), annotation );
				attributeOverrideList.put( 
						override.getAttributePath(), override );
			}
		}

		// Add all instances of @AttributeOverrides children
		final List<AnnotationInstance> attributeOverridesAnnotations = JandexHelper
				.getAnnotations( classInfo, JPADotNames.ATTRIBUTE_OVERRIDES );
		if ( attributeOverridesAnnotations != null ) {
			for ( AnnotationInstance attributeOverridesAnnotation : attributeOverridesAnnotations ) {
				final AnnotationInstance[] annotationInstances
						= attributeOverridesAnnotation.value().asNestedArray();
				for ( AnnotationInstance annotation : annotationInstances ) {
					final AttributeOverride override = new AttributeOverride(
							createPathPrefix(
									attributeOverridesAnnotation.target()
							),
							annotation
					);
					attributeOverrideList.put(
							override.getAttributePath(), override
					);
				}
			}
		}
		return attributeOverrideList;
	}

	private String createPathPrefix(AnnotationTarget target) {
		String prefix = null;
		if ( target instanceof FieldInfo || target instanceof MethodInfo ) {
			prefix = JandexHelper.getPropertyName( target );
		}
		return prefix;
	}

	protected String determineCustomTuplizer() {
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, HibernateDotNames.TUPLIZERS, ClassInfo.class
		);
		final AnnotationInstance tuplizerAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				HibernateDotNames.TUPLIZER,
				ClassInfo.class
		);
		return AnnotationParserHelper.determineCustomTuplizer( tuplizersAnnotation, tuplizerAnnotation );
	}

}
