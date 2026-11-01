package com.procureflow.identity.application.command;

public record RegisterUserCommand (String email,String rawPassword,String fullName){}
