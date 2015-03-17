package org.hibernate.jpa.test.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Cat extends Animal {

	private String furColor;

	@NotNull(groups = Special.class)
	public String getFurColor() {
		return furColor;
	}

	public void setFurColor(final String furColor) {
		this.furColor = furColor;
	}

}
