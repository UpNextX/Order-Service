package org.upnext.orderservice.Jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {
    Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);
    final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
         logger.error("Unauthorized access");
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final Map<String, Object> map = new HashMap<>();
        map.put("message", "Unauthorized access");
        map.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        map.put("error", authException.getMessage());
        map.put("path", request.getServletPath());
        mapper.writeValue(response.getOutputStream(), map);

    }
}
