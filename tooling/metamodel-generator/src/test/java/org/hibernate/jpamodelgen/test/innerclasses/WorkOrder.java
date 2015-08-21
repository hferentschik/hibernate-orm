package org.hibernate.jpamodelgen.test.innerclasses;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@SuppressWarnings("unused")
public class WorkOrder implements Serializable {

	private WorkOrderId id;

	@EmbeddedId
	public WorkOrderId getId() {
		return this.id;
	}

	public void setId(WorkOrderId id) {
		this.id = id;
	}

	@Embeddable
	public static class WorkOrderId implements Serializable {

		private String workOrder;
		private Long facilityId;

		public String getWorkOrder() {
			return this.workOrder;
		}

		public void setWorkOrder(String workOrder) {
			this.workOrder = workOrder;
		}

		public Long getFacilitityId() {
			return facilityId;
		}

		public void setFacilityId(Long facilityId) {
			this.facilityId = facilityId;
		}
	}
}
