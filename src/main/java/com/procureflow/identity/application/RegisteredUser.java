package com.procureflow.identity.application;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.UserId;
import com.procureflow.identity.domain.UserStatus;

public record RegisteredUser (UserId id, EmailAddress email , UserStatus status){
}
