package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.AccountRegistrationRequest;
import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AppUserRepository users;
    @Mock PasswordEncoder encoder;
    @InjectMocks AccountService service;

    @Test
    void registrationNormalizesEmailAndHashesPassword() {
        var request = new AccountRegistrationRequest("  Owner@Example.com  ", "S4 Owner", "long-unique-password");
        when(users.findByEmail("owner@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("long-unique-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(request);

        assertEquals("owner@example.com", response.email());
        verify(users).saveAndFlush(argThat(user ->
                user.getEmail().equals("owner@example.com")
                        && user.getPasswordHash().equals("bcrypt-hash")
                        && user.getDisplayName().equals("S4 Owner")));
    }

    @Test
    void duplicateEmailIsRejected() {
        var request = new AccountRegistrationRequest("owner@example.com", "S4 Owner", "long-unique-password");
        when(users.findByEmail("owner@example.com")).thenReturn(Optional.of(new AppUser()));

        var exception = assertThrows(ResponseStatusException.class, () -> service.register(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(users, never()).saveAndFlush(any());
    }
}
