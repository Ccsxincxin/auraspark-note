package com.auraspark.note.core.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws java.io.IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String ct = httpRequest.getContentType();
        if (ct != null && ct.contains("application/json")) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new XssRequestWrapper(httpRequest), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            for (int i = 0; i < values.length; i++) {
                values[i] = sanitize(values[i]);
            }
            return values;
        }
    }

    private static String sanitize(String input) {
        if (input == null) return null;
        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("&", "&amp;");
    }
}
