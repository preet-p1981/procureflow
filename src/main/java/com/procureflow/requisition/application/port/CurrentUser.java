package com.procureflow.requisition.application.port;

import java.util.UUID;

public interface CurrentUser {
    UUID id();
    boolean hasAuthority(String authority);
}