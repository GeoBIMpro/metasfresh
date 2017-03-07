package de.metas.handlingunits.report;

import java.util.Properties;

import org.adempiere.bpartner.service.IBPartnerBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.util.Env;
import org.compiere.util.Language;

import com.google.common.base.Preconditions;

import de.metas.handlingunits.model.I_M_ReceiptSchedule;
import de.metas.process.ProcessInfo;

/*
 * #%L
 * de.metas.handlingunits.base
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
 * This little class is specialized in executing the jasper process whose {@code AD_Process_ID} is specified in @c{@code SysConfig} {@value #SYSCONFIG_ReceiptScheduleHUPOSJasperProcess}
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class HUReceiptScheduleReportExecutor
{
	private static final String SYSCONFIG_ReceiptScheduleHUPOSJasperProcess = "ReceiptScheduleHUPOSJasperProcess";

	private static final String PARA_C_Orderline_ID = " C_Orderline_ID";
	private static final String PARA_AD_Table_ID = "AD_Table_ID";

	private final I_M_ReceiptSchedule receiptSchedule;

	private int windowNo = Env.WINDOW_None;

	private HUReceiptScheduleReportExecutor(final I_M_ReceiptSchedule receiptSchedule)
	{
		Preconditions.checkNotNull(receiptSchedule, "Param 'receiptSchedule' may not be null");
		this.receiptSchedule = receiptSchedule;
	};

	/**
	 * Creates an returns a new instance of this services which can execute the jasper report for the given {@code receiptSchedule}.
	 * 
	 * @param receiptSchedule
	 * @return
	 */
	public static HUReceiptScheduleReportExecutor get(final I_M_ReceiptSchedule receiptSchedule)
	{
		return new HUReceiptScheduleReportExecutor(receiptSchedule);
	}

	/**
	 * Give this service a window number. The default is {@link Env#WINDOW_None}.
	 *
	 * @param windowNo
	 * @return
	 */
	public HUReceiptScheduleReportExecutor withWindowNo(final int windowNo)
	{
		this.windowNo = windowNo;
		return this;
	}

	/**
	 *
	 */
	public void executeHUReport()
	{
		final I_C_OrderLine orderLine = receiptSchedule.getC_OrderLine();

		//
		// service
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);

		//
		// Get Process from SysConfig
		final String defaultValue = null;
		final String reportConfigValue = sysConfigBL.getValue(SYSCONFIG_ReceiptScheduleHUPOSJasperProcess,
				defaultValue,
				receiptSchedule.getAD_Client_ID(),
				receiptSchedule.getAD_Org_ID());
		Check.assumeNotNull(reportConfigValue, "Report SysConfig value not null for {}", SYSCONFIG_ReceiptScheduleHUPOSJasperProcess);

		final int reportProcessId = Integer.parseInt(reportConfigValue);

		final Properties ctx = InterfaceWrapperHelper.getCtx(receiptSchedule);

		Check.assumeNotNull(orderLine, "orderLine not null");
		final int orderLineId = orderLine.getC_OrderLine_ID();
		final I_C_Order order = orderLine.getC_Order();
		final Language bpartnerLaguage = Services.get(IBPartnerBL.class).getLanguage(ctx, order.getC_BPartner_ID());

		//
		// Create ProcessInfo

		ProcessInfo.builder()
				.setCtx(ctx)
				.setAD_Process_ID(reportProcessId)
				// .setAD_PInstance_ID() // NO AD_PInstance => we want a new instance
				.setRecord(I_C_OrderLine.Table_Name, orderLineId)
				.setWindowNo(windowNo)
				.setReportLanguage(bpartnerLaguage)
				.addParameter(PARA_C_Orderline_ID, orderLineId)
				.addParameter(PARA_AD_Table_ID, InterfaceWrapperHelper.getTableId(I_C_OrderLine.class))
				//
				// Execute report in a new AD_PInstance
				.buildAndPrepareExecution()
				.executeSync();
	}
}
