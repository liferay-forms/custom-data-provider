/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.data.provider.custom.current.user;

import com.google.gson.Gson;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderException;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderInstanceSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderOutputParametersSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.settings.DDMDataProviderSettingsProvider;
import com.liferay.dynamic.data.mapping.model.DDMDataProviderInstance;
import com.liferay.dynamic.data.mapping.service.DDMDataProviderInstanceService;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Carolina Barbosa
 */
@Component(
	immediate = true, property = "ddm.data.provider.type=user",
	service = DDMDataProvider.class
)
public class DDMCurrentUserDataProvider implements DDMDataProvider {

	@Override
	public DDMDataProviderResponse getData(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws DDMDataProviderException {

		try {
			return doGetData(ddmDataProviderRequest);
		}
		catch (Exception exception) {
			throw new DDMDataProviderException(exception);
		}
	}

	@Override
	public Class<?> getSettings() {
		return ddmDataProviderSettingsProvider.getSettings();
	}

	protected DDMDataProviderResponse createDDMDataProviderResponse(
			DDMCurrentUserDataProviderSettings
				ddmCurrentUserDataProviderSettings,
			JSONObject currentUserJSONObject)
		throws Exception {

		DDMDataProviderResponse.Builder builder =
			DDMDataProviderResponse.Builder.newBuilder();

		for (DDMDataProviderOutputParametersSettings outputParameterSettings :
				ddmCurrentUserDataProviderSettings.outputParameters()) {

			String outputParameterId =
				outputParameterSettings.outputParameterId();
			String outputParameterPath =
				outputParameterSettings.outputParameterPath();
			String outputParameterType =
				outputParameterSettings.outputParameterType();

			if (Objects.equals(outputParameterType, "text")) {
				builder = builder.withOutput(
					outputParameterId,
					currentUserJSONObject.getString(outputParameterPath));
			}
		}

		return builder.build();
	}

	protected DDMDataProviderResponse doGetData(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws Exception {

		Optional<DDMDataProviderInstance> ddmDataProviderInstance =
			fetchDDMDataProviderInstance(
				ddmDataProviderRequest.getDDMDataProviderId());

		DDMCurrentUserDataProviderSettings ddmCurrentUserDataProviderSettings =
			ddmDataProviderInstanceSettings.getSettings(
				ddmDataProviderInstance.get(),
				DDMCurrentUserDataProviderSettings.class);

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		User currentUser = permissionChecker.getUser();

		Gson gson = new Gson();

		JSONObject currentUserJSONObject = JSONFactoryUtil.createJSONObject(
			gson.toJson(currentUser));

		return createDDMDataProviderResponse(
			ddmCurrentUserDataProviderSettings, currentUserJSONObject);
	}

	protected Optional<DDMDataProviderInstance> fetchDDMDataProviderInstance(
			String ddmDataProviderInstanceId)
		throws Exception {

		DDMDataProviderInstance ddmDataProviderInstance =
			ddmDataProviderInstanceService.fetchDataProviderInstanceByUuid(
				ddmDataProviderInstanceId);

		if ((ddmDataProviderInstance == null) &&
			Validator.isNumber(ddmDataProviderInstanceId)) {

			ddmDataProviderInstance =
				ddmDataProviderInstanceService.fetchDataProviderInstance(
					Long.valueOf(ddmDataProviderInstanceId));
		}

		return Optional.ofNullable(ddmDataProviderInstance);
	}

	@Reference
	protected DDMDataProviderInstanceService ddmDataProviderInstanceService;

	@Reference
	protected DDMDataProviderInstanceSettings ddmDataProviderInstanceSettings;

	@Reference(target = "(ddm.data.provider.type=user)")
	protected DDMDataProviderSettingsProvider ddmDataProviderSettingsProvider;

}