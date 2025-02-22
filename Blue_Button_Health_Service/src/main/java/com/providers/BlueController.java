package com.providers;

import java.io.IOException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class BlueController {

	
    private String clientId = "zPc4ItwSvCnJF5HZGANi6Xmv7PQH65KI0h6xXKO6";
    private String clientSecret = "RuUM4jEhkogN9TEFGB1l4QXnMIit4Fq6AkIaAB3A2m8YWFSKQm55pWTUWfoNpRzWYN4RZ7koer6Q1TEqis8d4r8kramWPaQyI7wOlgcWFCf1J8TcN1hH4NQXA6eAVWTf"; // Remove leading space

    private String redirectUri = "https://oauth.pstmn.io/v1/browser-callback"; // Consistent redirect URI
    private String scope= "patient/Patient.read";
    
    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) throws IOException {
    	String authorizationUrl = "https://sandbox.bluebutton.cms.gov/v2/o/authorize"

    	+ "?response_type=code"

    	+ "&client_id=" + clientId

    	+ "&redirect_uri=" + redirectUri

    	+ "&scope= "+scope //patient/Patient.read  patient/Patient.read

    	+ "&state=1234" // Important for security

    	+ "&aud= https://sandbox.bluebutton.cms.gov/v2/fhir/";
        response.sendRedirect(authorizationUrl);
    }

    
    @GetMapping("/registered/callback")
    @ResponseBody
    
    
    
    public String handleCallback(@RequestParam("code") String authorizationCode, @RequestParam("state") String state) { // Include state parameter
        // Verify the state parameter!  This is crucial for preventing CSRF attacks.
        if (!state.equals("1234")) {
            return "Error: Invalid state parameter."; // Or handle differently
        }

        String tokenUrl = "https://sandbox.bluebutton.cms.gov/v2/o/token/";

        String requestBody = "grant_type=authorization_code"
                + "&code=" + authorizationCode
                + "&redirect_uri=" + redirectUri // Must match the redirect URI used in the authorize request
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                String accessToken = extractAccessToken(responseBody);

                if (accessToken != null) {
                    return getPatientData(accessToken); // Call a separate method for clarity
                } else {
                    return "Error: Could not extract access token. Response Body: " + responseBody;
                }

            } else {
                return "Error: Token exchange failed: " + response.getStatusCode() + " - " + response.getBody();
            }
        } catch (Exception e) {
            return "Error during token exchange: " + e.getMessage(); // More specific error handling
        }
    }


    private String getPatientData(String accessToken) {
        String patientApiUrl = "https://sandbox.bluebutton.cms.gov/v2/fhir/Patient";
        HttpHeaders patientApiHeaders = new HttpHeaders();
        patientApiHeaders.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> patientApiEntity = new HttpEntity<>(patientApiHeaders);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> patientApiResponse = restTemplate.exchange(patientApiUrl, HttpMethod.GET,
                    patientApiEntity, String.class);

            if (patientApiResponse.getStatusCode() == HttpStatus.OK) {
                return patientApiResponse.getBody();
            } else {
                return "Error: Patient API request failed: " + patientApiResponse.getStatusCode() + " - " + patientApiResponse.getBody();
            }
        } catch (Exception e) {
            return "Error during Patient API request: " + e.getMessage();
        }
    }

    private String extractAccessToken(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode accessTokenNode = jsonNode.get("access_token");
            return accessTokenNode != null ? accessTokenNode.asText() : null;
        } catch (Exception e) {
            e.printStackTrace(); // Keep for debugging, but log properly in production
            return null; // Important: Return null if parsing fails
        }
    }
}