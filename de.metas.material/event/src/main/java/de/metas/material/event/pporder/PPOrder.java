package de.metas.material.event.pporder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.compiere.model.I_S_Resource;

import de.metas.material.event.MaterialDescriptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.Wither;

/*
 * #%L
 * metasfresh-material-planning
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
@Data
@Builder
@Wither
@AllArgsConstructor // used by jackson when it deserializes a string
public class PPOrder
{
	@NonNull
	private final Integer orgId;

	/**
	 * The {@link I_S_Resource#getS_Resource_ID()} of the plant, as specified by the respective product planning record.
	 */
	@NonNull
	private final Integer plantId;

	@NonNull
	private final Integer warehouseId;

	@NonNull
	private final Integer productPlanningId;

	@NonNull
	private final Integer productId;

	/**
	 * See {@link MaterialDescriptor#getAttributeSetInstanceId()}.
	 */
	@NonNull
	private final Integer attributeSetInstanceId;
	
	/**
	 * See {@link MaterialDescriptor#getAsiKey()}.
	 */
	@NonNull
	private final String asiKey;
		
	@NonNull
	private final Integer uomId;

	/**
	 * In a build-to-order scenario, this is the ID of the order line which this all is about.
	 */
	private final int orderLineId;

	/**
	 * Can contain the {@code PP_Order_ID} of the production order document this is all about, but also note that there might not yet exist one.
	 */
	private final int ppOrderId;

	private final String docStatus;

	/**
	 * This is usually the respective supply candidates' date value.
	 */
	@NonNull
	private final Date datePromised;

	/**
	 * This is usually the respective demand candiates' date value.
	 */
	@NonNull
	private final Date dateStartSchedule;

	@NonNull
	private final BigDecimal quantity;

	/**
	 * If {@code true}, then this event advises the recipient to directly request an actual PP_Order to be created.
	 */
	private final boolean createPPOrder;

	/**
	 * Attention, might be {@code null}.
	 */
	@Singular
	private final List<PPOrderLine> lines;
}
