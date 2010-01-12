/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.soap.security.wss4j;

import java.util.Properties;

import org.apache.ws.security.components.crypto.Crypto;
import org.w3c.dom.Document;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.security.wss4j.support.CryptoFactoryBean;

public abstract class Wss4jMessageInterceptorSignTestCase extends Wss4jTestCase {

    protected Wss4jSecurityInterceptor interceptor;

    protected void onSetup() throws Exception {
        interceptor = new Wss4jSecurityInterceptor();
        interceptor.setValidationActions("Signature");

        CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        Properties cryptoFactoryBeanConfig = new Properties();
        cryptoFactoryBeanConfig.setProperty("org.apache.ws.security.crypto.provider",
                "org.apache.ws.security.components.crypto.Merlin");
        cryptoFactoryBeanConfig.setProperty("org.apache.ws.security.crypto.merlin.keystore.type", "jceks");
        cryptoFactoryBeanConfig.setProperty("org.apache.ws.security.crypto.merlin.keystore.password", "123456");

        // from the class path
        cryptoFactoryBeanConfig.setProperty("org.apache.ws.security.crypto.merlin.file", "private.jks");
        cryptoFactoryBean.setConfiguration(cryptoFactoryBeanConfig);
        cryptoFactoryBean.afterPropertiesSet();
        interceptor.setValidationSignatureCrypto((Crypto) cryptoFactoryBean
                .getObject());
        interceptor.setSecurementSignatureCrypto((Crypto) cryptoFactoryBean
                .getObject());
        interceptor.afterPropertiesSet();

    }

    public void testValidateCertificate() throws Exception {
        SoapMessage message = loadSoap11Message("signed-soap.xml");

        MessageContext messageContext = new DefaultMessageContext(message, getSoap11MessageFactory());
        interceptor.validateMessage(message, messageContext);
        Object result = getMessage(message);
        assertNotNull("No result returned", result);
        assertXpathNotExists("Security Header not removed", "/SOAP-ENV:Envelope/SOAP-ENV:Header/wsse:Security",
                getDocument(message));
    }

    public void testValidateCertificateWithSignatureConfirmation() throws Exception {
        SoapMessage message = loadSoap11Message("signed-soap.xml");
        MessageContext messageContext = getSoap11MessageContext(message);
        interceptor.setEnableSignatureConfirmation(true);
        interceptor.validateMessage(message, messageContext);
        WebServiceMessage response = messageContext.getResponse();
        interceptor.secureMessage(message, messageContext);
        assertNotNull("No result returned", response);
        Document document = getDocument((SoapMessage) response);
        assertXpathExists("Absent SignatureConfirmation element",
                "/SOAP-ENV:Envelope/SOAP-ENV:Header/wsse:Security/wsse11:SignatureConfirmation", document);
    }

    public void testSignResponse() throws Exception {
        interceptor.setSecurementActions("Signature");
        interceptor.setEnableSignatureConfirmation(false);
        interceptor.setSecurementPassword("123456");
        interceptor.setSecurementUsername("rsaKey");
        SoapMessage message = loadSoap11Message("empty-soap.xml");
        MessageContext messageContext = getSoap11MessageContext(message);

        // interceptor.setSecurementSignatureKeyIdentifier("IssuerSerial");

        interceptor.secureMessage(message, messageContext);

        Document document = getDocument(message);
        assertXpathExists("Absent SignatureConfirmation element",
                "/SOAP-ENV:Envelope/SOAP-ENV:Header/wsse:Security/ds:Signature", document);


    }

    public void testSignResponseWithSignatureUser() throws Exception {
        interceptor.setSecurementActions("Signature");
        interceptor.setEnableSignatureConfirmation(false);
        interceptor.setSecurementPassword("123456");
        interceptor.setSecurementSignatureUser("rsaKey");
        SoapMessage message = loadSoap11Message("empty-soap.xml");
        MessageContext messageContext = getSoap11MessageContext(message);

        interceptor.secureMessage(message, messageContext);

        Document document = getDocument(message);
        assertXpathExists("Absent SignatureConfirmation element",
                "/SOAP-ENV:Envelope/SOAP-ENV:Header/wsse:Security/ds:Signature", document);


    }
}
