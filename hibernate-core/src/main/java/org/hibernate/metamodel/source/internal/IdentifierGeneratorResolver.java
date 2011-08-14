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
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metamodel.binding.BasicAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.config.spi.ConfigurationService;

/**
 * @author Gail Badner
 */
public class IdentifierGeneratorResolver {

	private final MetadataImplementor metadata;

	IdentifierGeneratorResolver(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@SuppressWarnings( { "unchecked" })
	void resolve(Map<String, IdentifierGenerator> identifierGenerators) {
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			if ( !entityBinding.isRoot() ) {
				continue;
			}

			Properties properties = new Properties();
			properties.putAll(
					metadata.getServiceRegistry()
							.getService( ConfigurationService.class )
							.getSettings()
			);

			//TODO: where should these be added???
			if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
				properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
			}
			if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
				properties.put(
						PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
						new ObjectNameNormalizerImpl( metadata )
				);
			}

			EntityIdentifier entityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier();
			if ( entityIdentifier.getIdGenerator() != null && entityIdentifier.isSimple() ) {
				BasicAttributeBinding attributeBinding = ( (BasicAttributeBinding) entityIdentifier.getValueBinding() );
				IdentifierGenerator identifierGenerator = createIdentifierGenerator(
						attributeBinding,
						entityIdentifier.getIdGenerator(),
						metadata.getIdentifierGeneratorFactory(),
						properties
				);
				identifierGenerators.put( entityBinding.getEntity().getName(), identifierGenerator );
			}
		}
	}

	private IdentifierGenerator createIdentifierGenerator(
			BasicAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Properties properties) {
		Properties params = new Properties();
		params.putAll( properties );

		// use the schema/catalog specified by getValue().getTable() - but note that
		// if the schema/catalog were specified as params, they will already be initialized and
		//will override the values set here (they are in idGenerator.getParameters().)
		Schema schema = attributeBinding.getValue().getTable().getSchema();
		if ( schema != null ) {
			if ( schema.getName().getSchema() != null ) {
				params.setProperty( PersistentIdentifierGenerator.SCHEMA, schema.getName().getSchema().getName() );
			}
			if ( schema.getName().getCatalog() != null ) {
				params.setProperty( PersistentIdentifierGenerator.CATALOG, schema.getName().getCatalog().getName() );
			}
		}

		// TODO: not sure how this works for collection IDs...
		//pass the entity-name, if not a collection-id
		//if ( rootClass!=null) {
		params.setProperty(
				IdentifierGenerator.ENTITY_NAME,
				attributeBinding.getContainer().seekEntityBinding().getEntity().getName()
		);
		//}

		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//      splitting it up into schema/catalog/table names
		String tableName = attributeBinding.getValue()
				.getTable()
				.getQualifiedName( identifierGeneratorFactory.getDialect() );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		if ( attributeBinding.getSimpleValueSpan() > 1 ) {
			throw new MappingException(
					"A SimpleAttributeBinding used for an identifier has more than 1 Value: " + attributeBinding.getAttribute()
							.getName()
			);
		}
		SimpleValue simpleValue = (SimpleValue) attributeBinding.getValue();
		if ( !Column.class.isInstance( simpleValue ) ) {
			throw new MappingException(
					"Cannot create an IdentifierGenerator because the value is not a column: " +
							simpleValue.toLoggableString()
			);
		}
		params.setProperty(
				PersistentIdentifierGenerator.PK,
				( (Column) simpleValue ).getColumnName().encloseInQuotesIfQuoted(
						identifierGeneratorFactory.getDialect()
				)
		);

		// TODO: is this stuff necessary for SimpleValue???
		//if (rootClass!=null) {
		//	StringBuffer tables = new StringBuffer();
		//	Iterator iter = rootClass.getIdentityTables().iterator();
		//	while ( iter.hasNext() ) {
		//		Table table= (Table) iter.next();
		//		tables.append( table.getQuotedName(dialect) );
		//		if ( iter.hasNext() ) tables.append(", ");
		//	}
		//	params.setProperty( PersistentIdentifierGenerator.TABLES, tables.toString() );
		//}
		//else {
		params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
		//}

		params.putAll( idGenerator.getParameters() );

		return identifierGeneratorFactory.createIdentifierGenerator(
				idGenerator.getStrategy(),
				attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				params
		);
	}

	private static class ObjectNameNormalizerImpl extends ObjectNameNormalizer implements Serializable {
		private final boolean useQuotedIdentifiersGlobally;
		private final NamingStrategy namingStrategy;

		private ObjectNameNormalizerImpl(MetadataImplementor metadata) {
			this.useQuotedIdentifiersGlobally = metadata.isGloballyQuotedIdentifiers();
			this.namingStrategy = metadata.getNamingStrategy();
		}

		@Override
		protected boolean isUseQuotedIdentifiersGlobally() {
			return useQuotedIdentifiersGlobally;
		}

		@Override
		protected NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}
	}
}
