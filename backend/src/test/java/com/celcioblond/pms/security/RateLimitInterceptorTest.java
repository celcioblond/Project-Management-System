package com.celcioblond.pms.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private final MutableClock clock = new MutableClock();
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(2, clock);

    @Test
    void allowsRequestsUpToTheLimitThenAnswers429() {
        assertThat(call("alice").getStatus()).isEqualTo(200);
        assertThat(call("alice").getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = call("alice");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void limitIsTrackedPerUser() {
        call("alice");
        call("alice");

        assertThat(call("bob").getStatus()).isEqualTo(200);
    }

    @Test
    void windowResetsAfterOneMinute() {
        call("alice");
        call("alice");
        assertThat(call("alice").getStatus()).isEqualTo(429);

        clock.advance(Duration.ofSeconds(61));

        assertThat(call("alice").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse call(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(() -> username);
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());
        return response;
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
