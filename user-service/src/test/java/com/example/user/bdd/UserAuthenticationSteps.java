package com.example.user.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.user.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class UserAuthenticationSteps {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  private MvcResult latestResponse;

  @Before
  public void cleanUsers() {
    userRepository.deleteAll();
    latestResponse = null;
  }

  @When("I register a user with username {string}, password {string}, and role {string}")
  public void registerUser(String username, String password, String role) throws Exception {
    latestResponse =
        mockMvc
            .perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new RegistrationRequest(username, password, role))))
            .andReturn();
  }

  @Given("a registered user with username {string}, password {string}, and role {string}")
  public void registeredUser(String username, String password, String role) throws Exception {
    registerUser(username, password, role);
    assertEquals(201, latestResponse.getResponse().getStatus());
  }

  @When("I log in with username {string} and password {string}")
  public void login(String username, String password) throws Exception {
    latestResponse =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
  }

  @Then("the registration response status should be {int}")
  public void registrationStatus(int status) {
    assertEquals(status, latestResponse.getResponse().getStatus());
  }

  @Then("the registration response should contain username {string} and role {string}")
  public void registrationContains(String username, String role) throws Exception {
    JsonNode body = responseBody();
    assertEquals(username, body.get("username").asText());
    assertEquals(role, body.get("role").asText());
  }

  @Then("the registration response should contain a generated user id")
  public void registrationContainsGeneratedId() throws Exception {
    String userId = responseBody().get("userId").asText();
    assertNotNull(userId);
    assertTrue(userId.matches("[0-9a-fA-F-]{36}"));
  }

  @Then("the registration response should not contain a password")
  public void registrationDoesNotContainPassword() throws Exception {
    assertFalse(responseBody().has("password"));
    assertFalse(responseBody().has("passwordHash"));
  }

  @Then("the login response status should be {int}")
  public void loginStatus(int status) {
    assertEquals(status, latestResponse.getResponse().getStatus());
  }

  @Then("the login response should contain a Bearer access token")
  public void loginContainsBearerToken() throws Exception {
    JsonNode body = responseBody();
    assertEquals("Bearer", body.get("tokenType").asText());
    assertTrue(body.get("accessToken").asText().split("\\.").length == 3);
    assertTrue(body.get("expiresIn").asLong() > 0);
  }

  @Then("the access token should contain role {string}")
  public void tokenContainsRole(String role) throws Exception {
    assertEquals(role, tokenClaims().get("role").asText());
  }

  @Then("the access token should contain userId, issued-at, and expiration claims")
  public void tokenContainsRequiredClaims() throws Exception {
    JsonNode claims = tokenClaims();
    assertTrue(claims.hasNonNull("userId"));
    assertTrue(claims.hasNonNull("iat"));
    assertTrue(claims.hasNonNull("exp"));
    assertTrue(claims.get("exp").asLong() > claims.get("iat").asLong());
  }

  private JsonNode responseBody() throws Exception {
    return objectMapper.readTree(latestResponse.getResponse().getContentAsString());
  }

  private JsonNode tokenClaims() throws Exception {
    String token = responseBody().get("accessToken").asText();
    String payload = token.split("\\.")[1];
    byte[] decoded = Base64.getUrlDecoder().decode(payload);
    return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
  }

  private record RegistrationRequest(String username, String password, String role) {}

  private record LoginRequest(String username, String password) {}
}
