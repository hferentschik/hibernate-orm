package org.hibernate.jpa.test.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class C {
	@Id
	@GeneratedValue
	private long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTextC() {
		return textC;
	}

	public void setTextC(String textC) {
		this.textC = textC;
	}

	private String textC;

}
