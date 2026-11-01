package com.procureflow.identity.web.request;

import java.util.List;

public record ChangeRolesRequest(List<String> assign, List<String> remove) {}