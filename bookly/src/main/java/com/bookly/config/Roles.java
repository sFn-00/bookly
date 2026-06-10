package com.bookly.config;

public class Roles {
    public static final String OWNER_OR_STAFF = "hasRole('OWNER') or hasRole('STAFF')";
    public static final String OWNER_ONLY = "hasRole('OWNER')";
}
