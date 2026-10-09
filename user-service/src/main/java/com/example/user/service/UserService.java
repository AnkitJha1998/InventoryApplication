package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;

public interface UserService {
  UserResponses.Registration register(UserRequests.Registration request);
}
