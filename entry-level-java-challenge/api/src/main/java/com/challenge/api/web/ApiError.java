package com.challenge.api.web;

public record ApiError(int status, String error, String message, String path) {}
