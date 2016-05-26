/*
 * Copyright 2015 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.servlet.mvc;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.error.Error;
import com.stormpath.sdk.impl.error.DefaultError;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.resource.ResourceException;
import com.stormpath.sdk.servlet.filter.ForgotPasswordFilter;
import com.stormpath.sdk.servlet.form.DefaultField;
import com.stormpath.sdk.servlet.form.Field;
import com.stormpath.sdk.servlet.form.Form;
import com.stormpath.sdk.servlet.http.Resolver;
import com.stormpath.sdk.servlet.http.UserAgents;
import com.stormpath.sdk.servlet.i18n.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @since 1.0.RC4
 */
public class ChangePasswordController extends FormController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordFilter.class);

    private String forgotPasswordUri;
    private String loginUri;
    private String loginNextUri;
    private String errorUri;
    private Resolver<Locale> localeResolver;
    private MessageSource messageSource;
    private ErrorMapModelFactory errorMap;
    private boolean autologin;
    private AccountModelFactory accountMap;


    @Override
    public void init() {
        super.init();
        this.errorMap = new DefaultErrorMapModelFactory();
        this.accountMap = new DefaultAccountModelFactory();
        Assert.hasText(forgotPasswordUri, "forgotPasswordUri cannot be null or empty.");
        Assert.hasText(loginUri, "loginUri cannot be null or empty.");
        Assert.hasText(loginNextUri, "loginNextUri cannot be null or empty.");
        Assert.hasText(nextUri, "nextUri cannot be null or empty.");
        Assert.hasText(errorUri, "errorUri cannot be null or empty.");
        Assert.notNull(localeResolver, "localeResolver cannot be null.");
        Assert.notNull(messageSource, "messageSource cannot be null.");
    }

    @Override
    public boolean isNotAllowIfAuthenticated() {
        return false;
    }

    public String getForgotPasswordUri() {
        return forgotPasswordUri;
    }

    public void setForgotPasswordUri(String forgotPasswordUri) {
        this.forgotPasswordUri = forgotPasswordUri;
    }

    public String getLoginUri() {
        return loginUri;
    }

    public void setLoginUri(String loginUri) {
        this.loginUri = loginUri;
    }

    public String getLoginNextUri() {
        return loginNextUri;
    }

    public void setLoginNextUri(String loginNextUri) {
        this.loginNextUri = loginNextUri;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    public Resolver<Locale> getLocaleResolver() {
        return localeResolver;
    }

    public void setLocaleResolver(Resolver<Locale> localeResolver) {
        this.localeResolver = localeResolver;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    protected String i18n(HttpServletRequest request, String key) {
        Locale locale = localeResolver.get(request, null);
        return messageSource.getMessage(key, locale);
    }

    protected String i18n(HttpServletRequest request, String key, Object... args) {
        Locale locale = localeResolver.get(request, null);
        return messageSource.getMessage(key, locale, args);
    }

    public boolean isAutologin() {
        return autologin;
    }

    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
    }

    @Override
    protected ViewModel doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String sptoken = Strings.clean(request.getParameter("sptoken"));

        if (UserAgents.get(request).isJsonPreferred()) {
            Map<String, Object> model = new HashMap<String, Object>(1);
            if (sptoken == null) {
                model.put("status", "400");
                model.put("message", i18n(request, "stormpath.web.verify.emptyToken.error"));
            }
            else {
                try {
                    Application application = (Application) request.getAttribute(Application.class.getName());
                    application.verifyPasswordResetToken(sptoken);
                    model.put("status", "200");
                    model.put("message", "OK");
                }
                catch (ResourceException re) {
                    model = errorMap.toErrorMap(re.getStormpathError());
                }
            }
            return new DefaultViewModel("stormpathJsonView", model);
        }
        else {
            if (sptoken == null) {
                Map<String, String> queryParams = new HashMap<String, String>(1);
                queryParams.put("error", "sptokenInvalid");
                return new DefaultViewModel(getForgotPasswordUri(), queryParams).setRedirect(true);
            }
            else {
                try {
                    Application application = (Application) request.getAttribute(Application.class.getName());
                    application.verifyPasswordResetToken(sptoken);
                }
                catch (ResourceException re) {
                    return new DefaultViewModel(getErrorUri()).setRedirect(true);
                }
                return super.doGet(request, response);
            }

        }
    }

    @Override
    protected void appendModel(HttpServletRequest request, HttpServletResponse response, Form form, List<String> errors,
                               Map<String, Object> model) {
        model.put("loginUri", getLoginUri());
    }

    @Override
    //protected List<Field> createFields(HttpServletRequest request, boolean retainPassword, Form form) {
    protected List<Field> createFields(HttpServletRequest request, boolean retainPassword) {

        List<Field> fields;
        String[] fieldNames;

        if (UserAgents.get(request).isJsonPreferred()) {
            fields = new ArrayList<Field>(2);

            fieldNames = new String[]{"password", "sptoken"};

        }
        else {
            fields = new ArrayList<Field>(3);

            String value = Strings.clean(request.getParameter("sptoken"));

            if (value != null) {
                DefaultField field = new DefaultField();
                field.setName("sptoken");
                field.setType("hidden");
                field.setValue(value);
                fields.add(field);
            }

            fieldNames = new String[]{"password", "confirmPassword"};
        }

        for (String fieldName : fieldNames) {

            DefaultField field = new DefaultField();
            field.setName(fieldName);
            field.setName(fieldName);
            field.setLabel("stormpath.web.changePassword.form.fields." + fieldName + ".label");
            field.setPlaceholder("stormpath.web.changePassword.form.fields." + fieldName + ".placeholder");
            field.setRequired(true);
            field.setType("password");
            String val = fieldValueResolver.getValue(request, fieldName);
            field.setValue(retainPassword && val != null ? val : "");

            fields.add(field);
        }
        return fields;
    }

    @Override
    protected List<String> toErrors(HttpServletRequest request, Form form, Exception e) {

        List<String> errors = new ArrayList<String>(1);

        if (e instanceof IllegalArgumentException || e instanceof MismatchedPasswordException) {
            errors.add(e.getMessage());
        } else if (e instanceof ResourceException && ((ResourceException) e).getStatus() == 400) {
            //TODO: update this with a specific message that tells the exact password requirements.
            //This can only be done when this functionality is available via the Stormpath REST API
            //(currently being implemented in the Stormpath server-side REST API, not yet complete)
            String key = "stormpath.web.changePassword.form.errors.strength";
            String msg = i18n(request, key);
            errors.add(msg);
        } else if (e instanceof ResourceException && ((ResourceException) e).getCode() == 404) {
            String url = request.getContextPath() + getForgotPasswordUri();
            String key = "stormpath.web.changePassword.form.errors.invalid";
            String msg = i18n(request, key, url);
            errors.add(msg);
        } else {
            log.warn("Potentially unexpected change password problem.", e);
            String key = "stormpath.web.changePassword.form.errors.default";
            String msg = i18n(request, key);
            errors.add(msg);
        }

        return errors;
    }

    @Override
    protected ViewModel onValidSubmit(HttpServletRequest request, HttpServletResponse response, Form form)
        throws Exception {

        String password = form.getFieldValue("password");

        Application application = (Application) request.getAttribute(Application.class.getName());
        String sptoken = form.getFieldValue("sptoken");

        if (UserAgents.get(request).isJsonPreferred()) {
            Map<String, Object> model = new HashMap<String, Object>();
            try {
                Account account = application.resetPassword(sptoken, password);
                if (isAutologin()){
                    model.put("status", "200");
                    model.put("account", accountMap.toMap(account));                }
                else {
                    model.put("status", "200");
                    model.put("message", "OK");
                }
            }
            catch (ResourceException re) {
                model = errorMap.toErrorMap(re.getStormpathError());
            }
            catch (Exception e) {
                Map<String, Object> exceptionErrorMap = new HashMap<String, Object>();
                exceptionErrorMap.put("message", e.getMessage());
                exceptionErrorMap.put("status", 400);
                Error error = new DefaultError(exceptionErrorMap);
                model = errorMap.toErrorMap(error);
            }

            return new DefaultViewModel("stormpathJsonView", model);
        }
        else {
            application.resetPassword(sptoken, password);
            String next;
            if (isAutologin()){
                next = getLoginNextUri();
            }
            else{
                next = form.getNext();

                if (!Strings.hasText(next)) {
                    next = getNextUri();
                }
            }

            return new DefaultViewModel(next).setRedirect(true);
        }
    }

    protected void validate(HttpServletRequest request, HttpServletResponse response, Form form) {

        //validate CSRF
        validateCsrfToken(request, response, form);

        if (!UserAgents.get(request).isJsonPreferred()) {
            //ensure required fields are present:
            List<Field> fields = form.getFields();
            for (Field field : fields) {
                if (field.isRequired()) {
                    String value = Strings.clean(field.getValue());
                    if (value == null) {
                        String key = "stormpath.web.changePassword.form.fields." + field.getName() + ".required";
                        String msg = i18n(request, key);
                        throw new IllegalArgumentException(msg);
                    }
                }
            }

            //ensure fields match:
            String password = form.getFieldValue("password");
            String confirmPassword = form.getFieldValue("confirmPassword");

            if (!password.equals(confirmPassword)) {
                String key = "stormpath.web.changePassword.form.errors.mismatch";
                String msg = i18n(request, key);
                throw new MismatchedPasswordException(msg);
            }
        }
    }

    protected static class MismatchedPasswordException extends RuntimeException {
        public MismatchedPasswordException(String msg) {
            super(msg);
        }
    }
}
