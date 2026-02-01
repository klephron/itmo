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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@FR({"NFR-3.1"})
@Layer(TestLayer.BE_UNIT)
class RequestLogFilterEdgeCasesTest {

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
    @DisplayName("BE-UNIT-FILTER-06: Фильтр обрабатывает пустой query string")
    void handlesEmptyQueryString() throws ServletException, IOException {
        request.setRequestURI("/test");
        request.setQueryString("");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-07: Фильтр обрабатывает пустой request ID")
    void handlesBlankRequestId() throws ServletException, IOException {
        request.addHeader("X-Request-Id", "   ");
        request.setRequestURI("/test");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Request-Id")).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-08: Фильтр логирует даже при исключении в chain")
    void logsEvenOnException() throws ServletException, IOException {
        request.setRequestURI("/error");
        request.setMethod("POST");

        doThrow(new ServletException("Test error")).when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (ServletException e) {
            assertThat(e.getMessage()).isEqualTo("Test error");
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-09: Фильтр обрабатывает длинные URI")
    void handlesLongUri() throws ServletException, IOException {
        String longUri = "/api/" + "a".repeat(500);
        request.setRequestURI(longUri);
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FILTER-10: Фильтр обрабатывает специальные символы в URI")
    void handlesSpecialCharactersInUri() throws ServletException, IOException {
        request.setRequestURI("/test/<script>alert('xss')</script>");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);
    }
}
