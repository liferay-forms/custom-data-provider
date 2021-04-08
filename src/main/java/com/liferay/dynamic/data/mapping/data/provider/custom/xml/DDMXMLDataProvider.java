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

package com.liferay.dynamic.data.mapping.data.provider.custom.xml;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderException;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderInstanceSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderOutputParametersSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.settings.DDMDataProviderSettingsProvider;
import com.liferay.dynamic.data.mapping.model.DDMDataProviderInstance;
import com.liferay.dynamic.data.mapping.service.DDMDataProviderInstanceService;
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.Validator;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

/**
 * @author Carolina Barbosa
 */
@Component(
	immediate = true, property = "ddm.data.provider.type=xml",
	service = DDMDataProvider.class
)
public class DDMXMLDataProvider implements DDMDataProvider {

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
		DDMXMLDataProviderSettings ddmXMLDataProviderSettings,
		Document document) {

		DDMDataProviderResponse.Builder builder =
			DDMDataProviderResponse.Builder.newBuilder();

		for (DDMDataProviderOutputParametersSettings outputParameterSettings :
				ddmXMLDataProviderSettings.outputParameters()) {

			NodeList nodeList = document.getElementsByTagName(
				outputParameterSettings.outputParameterPath());

			List<KeyValuePair> keyValuePairs = new ArrayList<>();

			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);

				String nodeTextContent = node.getTextContent();

				keyValuePairs.add(
					new KeyValuePair(nodeTextContent, nodeTextContent));
			}

			builder = builder.withOutput(
				outputParameterSettings.outputParameterId(), keyValuePairs);
		}

		return builder.build();
	}

	protected DDMDataProviderResponse doGetData(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws Exception {

		Optional<DDMDataProviderInstance> ddmDataProviderInstance =
			fetchDDMDataProviderInstance(
				ddmDataProviderRequest.getDDMDataProviderId());

		DDMXMLDataProviderSettings ddmXMLDataProviderSettings =
			ddmDataProviderInstanceSettings.getSettings(
				ddmDataProviderInstance.get(),
				DDMXMLDataProviderSettings.class);

		HttpRequest httpRequest = HttpRequest.get(
			ddmXMLDataProviderSettings.url());

		httpRequest.trustAllCerts(true);

		HttpResponse httpResponse = httpRequest.send();

		httpResponse.charset("UTF-8");

		Document document = _convertXMLStringToDocument(
			httpResponse.bodyText());

		return createDDMDataProviderResponse(
			ddmXMLDataProviderSettings, document);
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

	@Reference(target = "(ddm.data.provider.type=xml)")
	protected DDMDataProviderSettingsProvider ddmDataProviderSettingsProvider;

	private Document _convertXMLStringToDocument(String xmlString)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			SecureXMLFactoryProviderUtil.newDocumentBuilderFactory();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		return documentBuilder.parse(
			new InputSource(new StringReader(xmlString)));
	}

}