package com.procureflow.identity.web.response;

import java.util.List;

public record MeResponse (String id, String email , List<String> authorities){
}
