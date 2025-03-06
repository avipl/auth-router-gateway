package com.authdemo.auth.controller;

import com.authdemo.auth.model.AuthTokens;
import com.authdemo.auth.service.JwtService;
import com.authdemo.auth.service.SignInService;
import com.authdemo.auth.entity.Role;
import com.authdemo.auth.entity.User;
import com.authdemo.auth.repository.UserRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static com.authdemo.auth.util.CookieUtil.addCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping(path = "/auth/v1/verify")
@Slf4j
@RequiredArgsConstructor
public class PhoneVerificationController {
    @Value("${twilio.ACCOUNT_SID}")
    private String ACCOUNT_SID;

    @Value("${twilio.AUTH_TOKEN}")
    private String AUTH_TOKEN;

    private String SID = "VAc6b509cc254275dc6057f29856a28240";

    private final JwtService jwtService;
    private final SignInService signInService;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping(value = "/generateOTP")
    public ResponseEntity<String> generateOTP(@RequestParam(required = true, name = "number") String number) {
        if(!validPhoneNumber(number)) {
            return ResponseEntity.status(BAD_REQUEST).body("Invalid number");
        }

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        User user = signInService.getCurrentUser();
        if(user.getVerifyTries() > 5) return  ResponseEntity.status(BAD_REQUEST).body("Max attempts reached. Please reach out to customer service");

        savePhoneNumber(user, number);
        Verification verification = Verification.creator(
                        SID,
                        user.getPhoneNumber(),
                        "sms")
                .create();

        log.info("OTP has been successfully generated, and awaits your verification {}", LocalDateTime.now());

        return new ResponseEntity<>("Your OTP has been sent to your verified phone number", HttpStatus.OK);
    }

    @GetMapping("/verifyOTP")
    public ResponseEntity verifyUserOTP(@RequestParam(required = true, name = "otp") String otp, HttpServletRequest request, HttpServletResponse response) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        User user = signInService.getCurrentUser();

        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                            SID)
                    .setTo(user.getPhoneNumber())
                    .setCode(otp)
                    .create();

            if(verificationCheck.getStatus().equals("approved")) {
                setUserVerified(user);
                updateContextHolder(request, response);
                AuthTokens authTokens = jwtService.generateAuthTokens(user);

                return ResponseEntity
                        .status(OK)
                        .header(SET_COOKIE, addCookie(AuthTokens.ACCESS_TOKEN_COOKIE_NAME, authTokens.accessToken(), authTokens.accessTokenTtl()).toString())
                        .body("This user's verification has been completed successfully");
            } else {
                ResponseEntity<String> verificationFailed = ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Verification failed");
                return verificationFailed;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Verification failed.", BAD_REQUEST);
        }
    }

    private void setUserVerified(User user) {
        user.setRole(Role.VERIFIED);
        userRepository.save(user);
    }

    private void updateContextHolder(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<GrantedAuthority> newAuthorities = new ArrayList<>(auth.getAuthorities());
        newAuthorities.remove(new SimpleGrantedAuthority(String.valueOf(Role.GUEST)));
        newAuthorities.add(new SimpleGrantedAuthority(String.valueOf(Role.VERIFIED)));

        Authentication newAuth = new PreAuthenticatedAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), newAuthorities);

        SecurityContextHolder.getContext().setAuthentication(newAuth);
        RequestContextHolder.currentRequestAttributes().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext(), RequestAttributes.SCOPE_SESSION);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
    }

    private Boolean validPhoneNumber(String number) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try{
            PhoneNumber phoneNumber = phoneUtil.parse(number, PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
            return phoneUtil.isValidNumber(phoneNumber);
        } catch (NumberParseException e) {
            log.error("Failed to parse number: " + e.toString());
            return false;
        }
    }

    private String getFormattedPhoneNumber(String number) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try{
            PhoneNumber phoneNumber = phoneUtil.parse(number, PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
            return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            log.error("Failed to parse number: " + e.toString());
            return "";
        }
    }

    private void savePhoneNumber(User user, String number) {
        String formattedNumber = getFormattedPhoneNumber(number);
        user.setPhoneNumber(formattedNumber);
        user.setVerifyTries(user.getVerifyTries() + 1);

        userRepository.save(user);
    }
}