package com.corporationxyz.config;

import com.corporationxyz.component.RateLimitInterceptor;
import com.corporationxyz.config.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.*;

class WebConfigTest {

    @Mock
    private RateLimitInterceptor rateLimitInterceptor;

    @InjectMocks
    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addInterceptors_ShouldRegisterRateLimitInterceptor() {
        // Use deep stubs to mock chained calls: addInterceptor().addPathPatterns()
        InterceptorRegistry registry = mock(InterceptorRegistry.class, RETURNS_DEEP_STUBS);

        webConfig.addInterceptors(registry);

        // Verify that addInterceptor() was called with our interceptor
        verify(registry, times(1)).addInterceptor(rateLimitInterceptor);

        // Verify that addPathPatterns("/api/**") was called on the returned InterceptorRegistration
        verify(registry.addInterceptor(rateLimitInterceptor), times(1))
                .addPathPatterns("/api/**");
    }
}
