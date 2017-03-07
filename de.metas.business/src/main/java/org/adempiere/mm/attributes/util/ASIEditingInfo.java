package org.adempiere.mm.attributes.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.api.IAttributeExcludeBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.GuavaCollectors;
import org.adempiere.util.LegacyAdapters;
import org.adempiere.util.Services;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_AttributeInstance;
import org.compiere.model.I_M_AttributeSet;
import org.compiere.model.I_M_AttributeSetExclude;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_ProductPrice;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MProduct;
import org.compiere.model.X_M_Attribute;
import org.compiere.util.Env;

import com.google.common.collect.ImmutableList;

import de.metas.product.IProductBL;

/*
 * #%L
 * de.metas.adempiere.adempiere.client
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

/**
 * Helper class used to provide informations to the ASI editor.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public final class ASIEditingInfo
{
	public static final ASIEditingInfo of( //
			final int productId, final int attributeSetInstanceId //
			, final String callerTableName, final int callerColumnId //
			, final boolean isSOTrx //
	)
	{
		return new ASIEditingInfo(productId, attributeSetInstanceId, callerTableName, callerColumnId, isSOTrx);
	}

	// Parameters
	private final WindowType _type;
	private final int _productId;
	private final int _attributeSetInstanceId;
	private final String _callerTableName;
	private final int _calledColumnId;
	private final boolean _isSOTrx;

	// Deducted values
	private final I_M_AttributeSet _attributeSet;
	private ImmutableList<MAttribute> _availableAttributes;
	private MAttributeSetInstance _attributeSetInstance;
	private final boolean _allowSelectExistingASI;
	private final boolean isLotEnabled;
	private final boolean isSerNoEnabled;
	private final boolean isGuaranteeDateEnabled;

	private ASIEditingInfo( //
			final int productId, final int attributeSetInstanceId //
			, final String callerTableName, final int callerColumnId //
			, final boolean isSOTrx //
	)
	{
		// Parameters, must be set first
		_type = extractType(callerTableName, callerColumnId);
		_productId = productId;
		_attributeSetInstanceId = attributeSetInstanceId;
		_callerTableName = callerTableName;
		_calledColumnId = callerColumnId;
		_isSOTrx = isSOTrx;

		// Deducted values, we assume params are set
		_attributeSet = retrieveM_AttributeSet();
		if (attributeSetInstanceId <= 0)
		{
			_attributeSetInstance = null;
		}
		else
		{
			final I_M_AttributeSetInstance asi = InterfaceWrapperHelper.create(Env.getCtx(), attributeSetInstanceId, I_M_AttributeSetInstance.class, ITrx.TRXNAME_None);
			_attributeSetInstance = LegacyAdapters.convertToPO(asi);
		}

		//
		// Flags
		_allowSelectExistingASI = _type == WindowType.Regular;

		isLotEnabled = _type == WindowType.Regular
				&& _attributeSet != null && _attributeSet.isLot();
		isSerNoEnabled = _type == WindowType.Regular
				&& _attributeSet != null && _attributeSet.isSerNo();
		// isGuaranteeDateEnabled:
		// We are displaying it if we deal with a pure product ASI (i.e. user is not editing the ASI from product window),
		// and if:
		// * the attribute set requires a GuaranteeDate
		// * or if the ASI has a GuaranteeDate already set
		isGuaranteeDateEnabled = _type == WindowType.Regular
				&& (_attributeSet != null && _attributeSet.isGuaranteeDate() || _attributeSetInstance != null && _attributeSetInstance.getGuaranteeDate() != null);
	}

	private static WindowType extractType(String callerTableName, final int callerColumnId)
	{
		if (I_M_Product.Table_Name.equals(callerTableName)) // FIXME HARDCODED: M_Product.M_AttributeSetInstance_ID's AD_Column_ID = 8418
		{
			return WindowType.ProductWindow;
		}
		else if (I_M_ProductPrice.Table_Name.equals(callerTableName)) // FIXME HARDCODED: M_ProductPrice.M_AttributeSetInstance_ID's AD_Column_ID = 556075
		{
			return WindowType.Pricing;
		}
		else if (Check.isEmpty(callerTableName, true))
		{
			return WindowType.ProcessParameter;
		}
		else
		{
			return WindowType.Regular;
		}
	}

	public boolean isAllowSelectExistingASI()
	{
		return _allowSelectExistingASI;
	}

	public WindowType getWindowType()
	{
		return _type;
	}

	private int getM_Product_ID()
	{
		return _productId;
	}

	private I_M_Product getM_Product()
	{
		final int M_Product_ID = getM_Product_ID();
		if (M_Product_ID <= 0)
		{
			return null;
		}
		return MProduct.get(Env.getCtx(), M_Product_ID);
	}

	public int getM_AttributeSetInstance_ID()
	{
		return _attributeSetInstanceId;
	}

	public MAttributeSetInstance getM_AttributeSetInstance()
	{
		return _attributeSetInstance;
	}

	public String getCallerTableName()
	{
		return _callerTableName;
	}

	public int getCallerColumnId()
	{
		return _calledColumnId;
	}

	public boolean isSOTrx()
	{
		return _isSOTrx;
	}

	@Nullable
	public I_M_AttributeSet getM_AttributeSet()
	{
		return _attributeSet;
	}

	public int getM_AttributeSet_ID()
	{
		final I_M_AttributeSet attributeSet = getM_AttributeSet();
		return attributeSet == null ? -1 : attributeSet.getM_AttributeSet_ID();
	}

	public String getM_AttributeSet_Name()
	{
		final I_M_AttributeSet attributeSet = getM_AttributeSet();
		return attributeSet == null ? "" : attributeSet.getName();
	}

	public String getM_AttributeSet_Description()
	{
		final I_M_AttributeSet attributeSet = getM_AttributeSet();
		return attributeSet == null ? "" : attributeSet.getDescription();
	}

	private I_M_AttributeSet retrieveM_AttributeSet()
	{
		final WindowType type = getWindowType();

		final I_M_AttributeSet attributeSet;
		switch (type)
		{
			case Regular:
			{
				attributeSet = getProductAttributeSet();
				// Product has no Instance Attributes
				if (attributeSet != null && !attributeSet.isInstanceAttribute())
				{
					throw new AdempiereException("@PAttributeNoInstanceAttribute@");
				}
				break;
			}
			case ProductWindow:
			{
				attributeSet = getProductAttributeSet();
				break;
			}
			case ProcessParameter:
			{
				final I_M_AttributeSet productAttributeSet = getProductAttributeSet();
				if (productAttributeSet != null)
				{
					attributeSet = productAttributeSet;
					break;
				}

				final MAttributeSetInstance asi = getM_AttributeSetInstance();
				attributeSet = asi == null ? null : asi.getM_AttributeSet();
				break;
			}
			case Pricing:
			{
				attributeSet = null;
				break;
			}
			default:
			{
				attributeSet = null;
				break;
			}
		}

		return attributeSet;
	}

	private I_M_AttributeSet getProductAttributeSet()
	{
		final I_M_Product product = getM_Product();
		if (product == null)
		{
			return null;
		}
		return Services.get(IProductBL.class).getM_AttributeSet(product);
	}

	public boolean isLotEnabled()
	{
		return isLotEnabled;
	}

	public boolean isSerNoEnabled()
	{
		return isSerNoEnabled;
	}

	public boolean isGuaranteeDateEnabled()
	{
		return isGuaranteeDateEnabled;
	}

	public boolean isExcludedAttributeSet()
	{
		final I_M_AttributeSet attributeSet = getM_AttributeSet();
		if (attributeSet != null && attributeSet.getM_AttributeSet_ID() > 0)
		{
			final IAttributeExcludeBL excludeBL = Services.get(IAttributeExcludeBL.class);
			final I_M_AttributeSetExclude asExclude = excludeBL.getAttributeSetExclude(attributeSet, getCallerColumnId(), isSOTrx());
			final boolean exclude = asExclude != null && excludeBL.isFullExclude(asExclude);
			return exclude;
		}

		// NOTE: at this point attributeSet is null or ID=0

		//
		// Regular window or product window requires a valid attributeSet
		final WindowType type = getWindowType();
		if (type == WindowType.Regular || type == WindowType.ProductWindow)
		{
			return true;
		}

		return false;
	}

	public List<MAttribute> getAvailableAttributes()
	{
		if (_availableAttributes == null)
		{
			_availableAttributes = retrieveAvailableAttributes();
		}
		return _availableAttributes;
	}

	public Set<Integer> getAvailableAttributeIds()
	{
		return getAvailableAttributes()
				.stream()
				.map(attribute -> attribute.getM_Attribute_ID())
				.collect(GuavaCollectors.toImmutableSet());
	}

	private ImmutableList<MAttribute> retrieveAvailableAttributes()
	{
		final WindowType type = getWindowType();
		final MAttributeSet attributeSet = LegacyAdapters.convertToPO(getM_AttributeSet());
		final int callerColumnId = getCallerColumnId();
		final boolean isSOTrx = isSOTrx();

		final Stream<MAttribute> attributes;
		switch (type)
		{
			case Regular:
			{
				attributes = retrieveAvailableAttributeSetAndInstanceAttributes(attributeSet, getM_AttributeSetInstance_ID())
						.stream();
				break;
			}
			case ProductWindow:
			{
				Check.assumeNotNull(attributeSet, "Parameter attributeSet is not null");
				attributes = Stream.of(attributeSet.getMAttributes(false)); // non-instance attributes
				break;
			}
			case ProcessParameter:
			{
				attributes = Services.get(IQueryBL.class)
						.createQueryBuilder(MAttribute.class, Env.getCtx(), ITrx.TRXNAME_None)
						.addOnlyActiveRecordsFilter()
						.addOnlyContextClient()
						//
						.orderBy()
						.addColumn(I_M_Attribute.COLUMNNAME_Name)
						.addColumn(I_M_Attribute.COLUMNNAME_M_Attribute_ID)
						.endOrderBy()
						//
						.create()
						.stream(MAttribute.class);
				break;
			}
			case Pricing:
			{
				attributes = Services.get(IQueryBL.class)
						.createQueryBuilder(MAttribute.class, Env.getCtx(), ITrx.TRXNAME_None)
						.addOnlyActiveRecordsFilter()
						.addOnlyContextClient()
						.addEqualsFilter(I_M_Attribute.COLUMNNAME_IsPricingRelevant, true)
						.addEqualsFilter(I_M_Attribute.COLUMNNAME_AttributeValueType, X_M_Attribute.ATTRIBUTEVALUETYPE_List) // atm only list attributes are supported, see IPricingAttribute
						//
						.orderBy()
						.addColumn(I_M_Attribute.COLUMNNAME_Name)
						.addColumn(I_M_Attribute.COLUMNNAME_M_Attribute_ID)
						.endOrderBy()
						//
						.create()
						.stream(MAttribute.class);
				break;
			}
			default:
			{
				return ImmutableList.of();
			}
		}

		final IAttributeExcludeBL attributeExcludeBL = Services.get(IAttributeExcludeBL.class);

		return attributes
				.filter(attribute -> attributeSet == null || !attributeExcludeBL.isExcludedAttribute(attribute, attributeSet, callerColumnId, isSOTrx))
				.collect(GuavaCollectors.toImmutableList());
	}

	/**
	 * 
	 * @param attributeSet
	 * @param attributeSetInstanceId
	 * @return list of available attributeSet's instance attributes, merged with the attributes which are currently present in our ASI (even if they are not present in attribute set)
	 */
	private static final List<MAttribute> retrieveAvailableAttributeSetAndInstanceAttributes(@Nullable final MAttributeSet attributeSet, final int attributeSetInstanceId)
	{
		final LinkedHashMap<Integer, MAttribute> attributes = new LinkedHashMap<>(); // preserve the order

		//
		// Retrieve attribute set's instance attributes,
		// and index them by M_Attribute_ID
		if (attributeSet != null)
		{
			Stream.of(attributeSet.getMAttributes(true))
					.forEach(attribute -> attributes.put(attribute.getM_Attribute_ID(), attribute));
		}

		//
		// If we have an ASI then fetch the attributes from ASI which are missing in attributeSet
		// and add them to our "attributes" index.
		if (attributeSetInstanceId > 0)
		{
			Services.get(IQueryBL.class)
					.createQueryBuilder(I_M_AttributeInstance.class, Env.getCtx(), ITrx.TRXNAME_None)
					.addEqualsFilter(I_M_AttributeInstance.COLUMN_M_AttributeSetInstance_ID, attributeSetInstanceId)
					//
					.andCollect(I_M_AttributeInstance.COLUMN_M_Attribute_ID)
					.addNotInArrayFilter(I_M_Attribute.COLUMN_M_Attribute_ID, attributes.keySet()) // skip already loaded attributes
					.orderBy()
					.addColumn(I_M_Attribute.COLUMN_Name)
					.addColumn(I_M_Attribute.COLUMN_M_Attribute_ID)
					.endOrderBy()
					//
					.create()
					.stream(MAttribute.class)
					.forEach(attribute -> attributes.put(attribute.getM_Attribute_ID(), attribute));
		}

		//
		return ImmutableList.copyOf(attributes.values());
	}

	public static enum WindowType
	{
		Regular, ProductWindow, ProcessParameter, Pricing,
	}
}
