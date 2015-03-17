package org.hibernate.jpa.test.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.Valid;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;

/**
 * @author Felix Feisst
 */
@Entity
@FetchProfile(name = "person-with-pet", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Person.class, association = "pet", mode = FetchMode.JOIN)
})
public class Person {
	private Long id;
	private Animal pet;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Valid
	@ManyToOne(fetch = FetchType.LAZY)
	public Animal getPet() {
		return pet;
	}

	public void setPet(final Animal pet) {
		this.pet = pet;
	}

}
