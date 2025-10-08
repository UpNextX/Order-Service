package org.upnext.orderservice.Errors;

import org.upnext.sharedlibrary.Errors.Error;

public class AuthErros {
    public static final Error UnauthorizedUser = new Error("USER.UnauthorizedUser", "UNAUTHORIZED", 401);
    public static final Error UnauthenticatedUser = new Error("USER.UnauthenticatedUser", "UNAUTHENTICATED", 403);

}
