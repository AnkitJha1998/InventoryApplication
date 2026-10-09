package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;

public interface AuthService {
  UserResponses.Login login(UserRequests.Login request)
      throws UserExceptions.InvalidCredentialsException;
}
