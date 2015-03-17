package org.hibernate.jpa.test.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Felix Feisst
 */
@Entity
public abstract class Animal {
	private Long id;
	private String name;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@NotNull
	@Size(min = 1)
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

}
