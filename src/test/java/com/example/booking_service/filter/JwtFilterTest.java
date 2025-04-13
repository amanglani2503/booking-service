package com.example.user_service.filter;

import com.example.user_service.model.Role;
import com.example.user_service.model.User;
import com.example.user_service.model.UserPrincipal;
import com.example.user_service.service.JWTService;
import com.example.user_service.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

class JwtFilterTest {

    @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private JWTService jwtService;

    @Mock
    private ApplicationContext context;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_ValidToken_SetsAuthentication() throws Exception {
        String token = "valid.token.value";
        String username = "test@example.com";

        User mockUser = new User();
        mockUser.setEmail(username);
        mockUser.setPassword("encodedpass");
        mockUser.setRole(Role.PASSENGER);

        UserPrincipal userDetails = new UserPrincipal(mockUser);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(context.getBean(UserDetailsServiceImpl.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth instanceof UsernamePasswordAuthenticationToken;
        assert auth.getPrincipal().equals(userDetails);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_InvalidToken_DoesNotAuthenticate() throws Exception {
        String token = "invalid.token";
        String username = "test@example.com";

        UserPrincipal dummyDetails = mock(UserPrincipal.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(context.getBean(UserDetailsServiceImpl.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(dummyDetails);
        when(jwtService.validateToken(token, dummyDetails)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assert SecurityContextHolder.getContext().getAuthentication() == null;
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_NoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assert SecurityContextHolder.getContext().getAuthentication() == null;
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ExceptionDuringProcessing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some.token");
        when(jwtService.extractUsername(any())).thenThrow(new RuntimeException("Boom"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Still calls filter chain even if exception occurs
        verify(filterChain).doFilter(request, response);
    }
}
