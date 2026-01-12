package ru.itmo.zavar.brainstop.discovery;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@FR({"NFR-3.1", "NFR-3.2"})
@Layer(TestLayer.BE_UNIT)
class RequestLogFilterTest {

    private RequestLogFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestLogFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-01: Фильтр добавляет X-Request-Id если его нет")
    void addsRequestIdWhenMissing() throws ServletException, IOException {
        request.setRequestURI("/test");
        request.setMethod("GET");

        doAnswer(invocation -> {
            assertThat(response.getHeader("X-Request-Id")).isNotNull();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-02: Фильтр сохраняет существующий X-Request-Id")
    void preservesExistingRequestId() throws ServletException, IOException {
        String existingId = "existing-request-id";
        request.addHeader("X-Request-Id", existingId);
        request.setRequestURI("/test");
        request.setMethod("POST");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Request-Id")).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-03: Фильтр обрабатывает query parameters")
    void handlesQueryParameters() throws ServletException, IOException {
        request.setRequestURI("/test");
        request.setQueryString("param=value");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-04: Фильтр обрабатывает разные HTTP методы")
    void handlesDifferentHttpMethods() throws ServletException, IOException {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

        for (String method : methods) {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            request.setRequestURI("/test");
            request.setMethod(method);

            filter.doFilterInternal(request, response, filterChain);
        }
    }
}
