package org.hibernate.jpa.test.beanvalidation;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class B {
	@Id
	@GeneratedValue
	private long id;

	private String textB;

	@ManyToMany(fetch = FetchType.LAZY)
	private Set<C> c;

	public Set<C> getC() {
		return c;
	}

	public void setC(Set<C> c) {
		this.c = c;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTextB() {
		return textB;
	}

	public void setTextB(String textB) {
		this.textB = textB;
	}
}
