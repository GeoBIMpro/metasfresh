package de.metas.handlingunits.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.adempiere.util.lang.IMutable;
import org.compiere.model.I_AD_Process;
import org.compiere.util.Env;
import org.slf4j.Logger;

import de.metas.handlingunits.HUIteratorListenerAdapter;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.impl.HUIterator;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.process.api.IMHUProcessBL;
import de.metas.logging.LogManager;
import de.metas.process.IADProcessDAO;

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
 * This service facade offers useful methods around to handling unit related jasper report processed.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class HUReportService
{
	private static final transient Logger logger = LogManager.getLogger(HUReportService.class);

	public static final String SYSCONFIG_RECEIPT_LABEL_PROCESS_ID = "de.metas.handlingunits.MaterialReceiptLabel.AD_Process_ID";

	public static final String SYSCONFIG_RECEIPT_LABEL_AUTO_PRINT_ENABLED = "de.metas.handlingunits.MaterialReceiptLabel.AutoPrint.Enabled";

	public static final String SYSCONFIG_RECEIPT_LABEL_AUTO_PRINT_COPIES = "de.metas.handlingunits.MaterialReceiptLabel.AutoPrint.Copies";

	private static final HUReportService INSTANCE = new HUReportService();

	public static HUReportService get()
	{
		return INSTANCE;
	}

	/**
	 * Return the {@link I_AD_Process} that was configured via {@code SysConfig} {@value #SYSCONFIG_RECEIPT_LABEL_AUTO_PRINT_PROCESS_ID}, if any.
	 *
	 * @param ctx use to get {@code AD_Client_ID} and {@code AD_Org_ID} for the sysconfig call.
	 * @return
	 */
	public Optional<I_AD_Process> retrievePrintReceiptLabelProcess(final Properties ctx)
	{
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);
		final int reportProcessId = sysConfigBL.getIntValue(SYSCONFIG_RECEIPT_LABEL_PROCESS_ID, -1, Env.getAD_Client_ID(ctx), Env.getAD_Org_ID(ctx));

		if (reportProcessId <= 0)
		{
			return Optional.empty(); // nothing to do here
		}

		final IADProcessDAO adProcessDAO = Services.get(IADProcessDAO.class);
		final I_AD_Process process = adProcessDAO.retrieveProcessById(ctx, reportProcessId);
		return Optional.of(process);
	}

	/**
	 * For the given {@code hu} and {@code process} this method traverses the HU hierarchy (using the hu as root) and collects every HU that is a fit for the process according to {@link IMHUProcessBL#processFitsType(I_AD_Process, String)}.
	 * 
	 * @param hu
	 * @param process
	 * @return never {@code null}
	 */
	public List<I_M_HU> getHUsToProcess(final I_M_HU hu, final I_AD_Process process)
	{
		final List<I_M_HU> husToProcess = new ArrayList<>();

		final IMHUProcessBL huProcessBL = Services.get(IMHUProcessBL.class);
		final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);

		logger.info("Looking for processable HUs belong hu={}", hu);
		new HUIterator()
				.setEnableStorageIteration(false) // gh metasfresh-webui#222: we only care for HUs. Also note that to iterate storages, we would have to provide a date.
				.setListener(new HUIteratorListenerAdapter()
				{
					@Override
					public Result beforeHU(final IMutable<I_M_HU> hu)
					{
						final String huType = handlingUnitsBL.getHU_UnitType(hu.getValue());
						if (huProcessBL.processFitsType(process, huType))
						{
							logger.debug("process.value={}: add hu with type={} to husToProcess; hu={}", process.getValue(), huType, hu);
							husToProcess.add(hu.getValue());
						}
						else
						{
							logger.debug("process.value={}: DO NOT add hu with type={} to husToProcess; hu={}", process.getValue(), huType, hu);
						}
						return getDefaultResult();
					}
				})
				.iterate(hu);

		return husToProcess;
	}

	public int getReceiptLabelAutoPrintCopyCount(final Properties ctx)
	{
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);

		final int copies = sysConfigBL.getIntValue(SYSCONFIG_RECEIPT_LABEL_AUTO_PRINT_COPIES, 1, Env.getAD_Client_ID(ctx), Env.getAD_Org_ID(ctx));
		return copies;
	}

	public boolean isReceiptLabelAutoPrintEnabled(final Properties ctx)
	{
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);

		final boolean enabled = sysConfigBL.getBooleanValue(SYSCONFIG_RECEIPT_LABEL_AUTO_PRINT_ENABLED, false, Env.getAD_Client_ID(ctx), Env.getAD_Org_ID(ctx));
		return enabled;
	}

}
