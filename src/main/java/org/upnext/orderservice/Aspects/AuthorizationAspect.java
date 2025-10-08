package org.upnext.orderservice.Aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.upnext.sharedlibrary.Dtos.UserDto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.upnext.orderservice.Utils.UserExtractor.userExtractor;
import org.upnext.orderservice.Annotations.RequiredRole;

@Aspect
@Component
public class AuthorizationAspect {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(requireRole)")
    public Object authorize(ProceedingJoinPoint joinPoint, RequiredRole requireRole) throws Throwable {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        UserDto user = userExtractor(request);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }
        String role = requireRole.value().toUpperCase();

        if(!role.equals("AUTHENTICATED") && !user.getRole().equals(role)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("User is not authorized");
        }
        return joinPoint.proceed();
    }


}
