package com.stormpath.spring.mvc;

import org.springframework.beans.factory.annotation.Value;

/**
 * @since 1.0.0
 */
public class ChangePasswordControllerConfigResolver extends  AbstractSpringControllerConfigResolver {
    @Value("#{ @environment['stormpath.web.changePassword.enabled'] ?: true }")
    protected boolean changePasswordEnabled;

    @Value("#{ @environment['stormpath.web.changePassword.uri'] ?: '/change' }")
    protected String changePasswordUri;

    @Value("#{ @environment['stormpath.web.changePassword.nextUri'] ?: '/login?status=changed' }")
    protected String changePasswordNextUri;

    @Value("#{ @environment['stormpath.web.changePassword.view'] ?: 'stormpath/change' }")
    protected String changePasswordView;

    @Override
    public String getView() {
        return changePasswordView;
    }

    @Override
    public String getUri() {
        return changePasswordUri;
    }

    @Override
    public String getNextUri() {
        return changePasswordNextUri;
    }

    @Override
    public boolean isEnabled() {
        return changePasswordEnabled;
    }

    @Override
    public String getControllerKey() {
        return "changePassword";
    }

    @Override
    protected String[] getDefaultFieldOrder() {
        return new String[0];
    }
}
