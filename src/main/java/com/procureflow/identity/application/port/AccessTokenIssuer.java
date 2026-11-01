package com.procureflow.identity.application.port;

import java.rmi.registry.LocateRegistry;
import java.util.Collection;

public interface AccessTokenIssuer {
    AccessToken issue(String subject , String email, Collection<String> authorities);

}
