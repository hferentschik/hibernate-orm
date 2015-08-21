package org.hibernate.jpa.test.beanvalidation;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class A {
	@Id
	@GeneratedValue
	private long id;

	private String textA;

	@ManyToMany(fetch = FetchType.LAZY)
	@NotNull
	private Set<B> b;

	public Set<B> getB() {
		return b;
	}

	public void setB(Set<B> b) {
		this.b = b;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTextA() {
		return textA;
	}

	public void setTextA(String textA) {
		this.textA = textA;
	}
}
