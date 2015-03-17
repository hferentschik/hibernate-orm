package org.hibernate.jpa.test.beanvalidation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.emops.Pet;

/**
 * @author Felix Feisst
 */
public class ValidatorTest extends BaseEntityManagerFunctionalTestCase {

	private Person person;
	private EntityManager entityManager;

	@Before
	public void setUp() {
		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		Cat cat = new Cat();
		cat.setName( "cat" );
		cat.setFurColor( null ); // special constraint is violated
		entityManager.persist( cat );

		person = new Person();
		person.setPet( cat );
		entityManager.persist( person );

		entityManager.getTransaction().commit();
		entityManager.clear();
	}

	@Test
	public void testValidationWithEntityManagerEntityGraph() {
		EntityGraph<Person> graph = entityManager.createEntityGraph( Person.class );
		graph.addAttributeNodes( "pet" );

		Map<String, Object> hints = new HashMap<String, Object>();
		hints.put( "javax.persistence.loadgraph", graph );

		Person reloaded = entityManager.find( Person.class, person.getId(), hints );

		runValidation( reloaded );
	}


	@Test
	public void testValidationWithJPACriteria() {
		CriteriaQuery<Person> criteriaQuery = entityManager.getCriteriaBuilder().createQuery( Person.class );
		// TODO the query would need further selection criteria
		Root<Person> person = criteriaQuery.from( Person.class );
		person.fetch( "pet" );
		criteriaQuery.select( person );

		List<Person> personList = entityManager.createQuery( criteriaQuery ).getResultList();
		runValidation( personList.get( 0 ) );
	}

	@Test
	public void testValidationWithHibernateSessionFetchProfile() {
		Session session = entityManager.unwrap( Session.class );
		session.enableFetchProfile( "person-with-pet" );

		Person reloaded = (Person) session.get( Person.class, person.getId() );
		runValidation( reloaded );
	}

	@Test
	public void testValidationWithHibernateSessionCriteria() {
		Session session = entityManager.unwrap( Session.class );
		Criteria criteria = session.createCriteria( Person.class );
		criteria.setFetchMode( "pet", FetchMode.JOIN );
		criteria.add( Restrictions.eq( "id", person.getId() ) );
		List personList = criteria.list();

		runValidation( (Person) personList.get( 0 ) );
	}

	private void runValidation(Person reloaded) {
		ValidatorFactory validatorFactory = Validation
				.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		Set<ConstraintViolation<Person>> constraints = validator
				.validate( reloaded, Default.class, Special.class );
		assertEquals( "Expected exactly one constraint violation", 1, constraints.size() );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class, Cat.class, Person.class, Special.class
		};
	}
}
