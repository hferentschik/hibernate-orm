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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbAnyElement;
import org.hibernate.jaxb.spi.hbm.JaxbBagElement;
import org.hibernate.jaxb.spi.hbm.JaxbComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbDynamicComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbIdbagElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbListElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapElement;
import org.hibernate.jaxb.spi.hbm.JaxbOneToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSetElement;
import org.hibernate.jaxb.spi.hbm.JaxbSynchronizeElement;
import org.hibernate.jaxb.spi.hbm.JaxbTuplizerElement;
import org.hibernate.jaxb.spi.hbm.JoinElementSource;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractEntitySourceImpl
		extends AbstractHbmSourceNode
		implements EntitySource, Helper.InLineViewNameInferrer {

	private final EntityElement entityElement;
	private final String className;
	private final String entityName;

	private List<SubclassEntitySource> subclassEntitySources = new ArrayList<SubclassEntitySource>();

	private int inLineViewCount = 0;

	// logically final, but built during 'afterInstantiation' callback
	private List<AttributeSource> attributeSources;
	private Set<SecondaryTableSource> secondaryTableSources;

	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		super( sourceMappingDocument );
		this.entityElement = entityElement;

		this.className = bindingContext().qualifyClassName( entityElement.getName() );
		this.entityName = StringHelper.isNotEmpty( entityElement.getEntityName() )
				? entityElement.getEntityName()
				: className;
	}

	@Override
	public String inferInLineViewName() {
		return entityName + '#' + (++inLineViewCount);
	}

	protected void afterInstantiation() {
		this.attributeSources = buildAttributeSources();
		this.secondaryTableSources = buildSecondaryTables();
	}

	protected List<AttributeSource> buildAttributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		buildAttributeSources( attributeSources );
		return attributeSources;
	}

	protected List<AttributeSource> buildAttributeSources(List<AttributeSource> attributeSources) {
		return buildAttributeSources( entityElement, attributeSources, null, SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID );
	}
	protected List<AttributeSource> buildAttributeSources(EntityElement element,
														  List<AttributeSource> attributeSources,
														  String logicTalbeName,
														  SingularAttributeBinding.NaturalIdMutability naturalIdMutability){
		processPropertyAttributes( attributeSources, element.getProperty(), logicTalbeName, naturalIdMutability );
		processComponentAttributes(
				attributeSources,
				element.getComponent(),
				logicTalbeName,
				naturalIdMutability
		);
		processDynamicComponentAttributes(
				attributeSources,
				element.getDynamicComponent(),
				logicTalbeName,
				naturalIdMutability
		);
		processManyToOneAttributes(
				attributeSources,
				element.getManyToOne(),
				logicTalbeName,
				naturalIdMutability
		);
		processOneToOneAttributes(
				attributeSources,
				element.getOneToOne(),
				logicTalbeName,
				naturalIdMutability
		);
		processAnyAttributes(
				attributeSources,
				element.getAny(),
				logicTalbeName,
				naturalIdMutability
		);
		processMapAttributes( attributeSources, element.getMap() );
		processListAttributes( attributeSources, element.getList() );
		processSetAttributes( attributeSources, element.getSet() );
		processIdBagAttributes( attributeSources, element.getIdbag() );
		processBagAttributes( attributeSources, element.getBag() );
		return attributeSources;
	}

	protected void processPropertyAttributes(List<AttributeSource> results,
											 List<JaxbPropertyElement> propertyElements,
											 String logicalTableName,
											 SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbPropertyElement element : propertyElements ) {
			results.add(
					new PropertyAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}

	protected void processComponentAttributes(List<AttributeSource> results,
											 List<JaxbComponentElement> elements,
											 String logicalTableName,
											 SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbComponentElement element : elements ) {
			results.add(
					new ComponentAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							this,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}

	protected void processDynamicComponentAttributes(List<AttributeSource> results,
											  List<JaxbDynamicComponentElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		// todo : implement
	}

	protected void processManyToOneAttributes(List<AttributeSource> results,
											  List<JaxbManyToOneElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbManyToOneElement element : elements ) {
			results.add(
					new ManyToOneAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}
	protected void processOneToOneAttributes(List<AttributeSource> results,
											   List<JaxbOneToOneElement> elements,
											   String logicalTableName,
											   SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		// todo : implement
	}

	protected void processAnyAttributes(List<AttributeSource> results,
											  List<JaxbAnyElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		// todo : implement
	}

	protected void processMapAttributes(List<AttributeSource> results,
											 List<JaxbMapElement> propertyElements){
		for ( JaxbMapElement element : propertyElements ) {
			results.add(
					new MapAttributeSourceImpl(
							sourceMappingDocument(),
							element, this
					)
			);
		}
	}
	protected void processListAttributes(List<AttributeSource> results,
											 List<JaxbListElement> propertyElements){
		for ( JaxbListElement element : propertyElements ) {
			results.add(
					new ListAttributeSourceImpl(
							sourceMappingDocument(),
							element, this
					)
			);
		}
	}
	protected void processSetAttributes(List<AttributeSource> results,
											 List<JaxbSetElement> propertyElements){
		for ( JaxbSetElement element : propertyElements ) {
			results.add(
					new SetAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							this
					)
			);
		}
	}
	protected void processIdBagAttributes(List<AttributeSource> results,
											 List<JaxbIdbagElement> propertyElements){
		// todo : implement
	}
	protected void processBagAttributes(List<AttributeSource> results,
											 List<JaxbBagElement> propertyElements) {
		for ( JaxbBagElement element : propertyElements ) {
			results.add(
					new BagAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							this
					)
			);
		}
	}


	private Set<SecondaryTableSource> buildSecondaryTables() {
		if ( ! JoinElementSource.class.isInstance( entityElement ) ) {
			return Collections.emptySet();
		}

		final Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();
		for ( JaxbJoinElement joinElement :  ( (JoinElementSource) entityElement ).getJoin() ) {
			final SecondaryTableSourceImpl secondaryTableSource = new SecondaryTableSourceImpl(
					sourceMappingDocument(),
					joinElement,
					this
			);
			secondaryTableSources.add( secondaryTableSource );

			final String logicalTableName = secondaryTableSource.getLogicalTableNameForContainedColumns();
			final SingularAttributeBinding.NaturalIdMutability  naturalIdMutability = SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
			processAnyAttributes(
					attributeSources,
					joinElement.getAny(),
					logicalTableName,
					naturalIdMutability
			);
			processComponentAttributes(
					attributeSources,
					joinElement.getComponent(),
					logicalTableName,
					naturalIdMutability
			);
			processDynamicComponentAttributes(
					attributeSources,
					joinElement.getDynamicComponent(),
					logicalTableName,
					naturalIdMutability
			);
			processManyToOneAttributes(
					attributeSources,
					joinElement.getManyToOne(),
					logicalTableName,
					naturalIdMutability
			);
			processPropertyAttributes(
					attributeSources,
					joinElement.getProperty(),
					logicalTableName,
					naturalIdMutability
			);
		}
		return secondaryTableSources;
	}

	protected EntityElement entityElement() {
		return entityElement;
	}

	@Override
	public Origin getOrigin() {
		return origin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext();
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getJpaEntityName() {
		return null;
	}

	@Override
	public boolean isAbstract() {
		return entityElement().isAbstract();
	}

	@Override
	public boolean isLazy() {
		return entityElement().isLazy();
	}

	@Override
	public String getProxy() {
		return entityElement.getProxy();
	}

	@Override
	public int getBatchSize() {
		return entityElement.getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return entityElement.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return entityElement.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return entityElement.isSelectBeforeUpdate();
	}

	protected EntityMode determineEntityMode() {
		return StringHelper.isNotEmpty( getClassName() ) ? EntityMode.POJO : EntityMode.MAP;
	}

	@Override
	public String getCustomTuplizerClassName() {
		if ( entityElement.getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = determineEntityMode();
		for ( JaxbTuplizerElement tuplizerElement : entityElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode().value() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public String getCustomPersisterClassName() {
		return getLocalBindingContext().qualifyClassName( entityElement.getPersister() );
	}

	@Override
	public String getCustomLoaderName() {
		return entityElement.getLoader() != null ? entityElement.getLoader().getQueryRef() : null;
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return Helper.buildCustomSql( entityElement.getSqlInsert() );
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return Helper.buildCustomSql( entityElement.getSqlUpdate() );
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return Helper.buildCustomSql( entityElement.getSqlDelete() );
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		List<String> tableNames = new ArrayList<String>();
		for ( JaxbSynchronizeElement synchronizeElement : entityElement.getSynchronize() ) {
			tableNames.add( synchronizeElement.getTable() );
		}
		return tableNames;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return entityElement.getMeta();
	}

	@Override
	public String getPath() {
		return bindingContext().determineEntityName( entityElement );
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	private EntityHierarchyImpl entityHierarchy;

	public void injectHierarchy(EntityHierarchyImpl entityHierarchy) {
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	public void add(SubclassEntitySource subclassEntitySource) {
		add( (SubclassEntitySourceImpl) subclassEntitySource );
	}

	public void add(SubclassEntitySourceImpl subclassEntitySource) {
		entityHierarchy.processSubclass( subclassEntitySource );
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public Iterable<SubclassEntitySource> subclassEntitySources() {
		return subclassEntitySources;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return null;
	}

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		return Collections.emptySet();
	}

	@Override
	public Set<SecondaryTableSource> getSecondaryTables() {
		return secondaryTableSources;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public List<JpaCallbackSource> getJpaCallbackClasses() {
	    return Collections.EMPTY_LIST;
	}
}
