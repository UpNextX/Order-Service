package org.upnext.orderservice.Configurations;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        System.out.println("FeignClientConfiguration.requestInterceptor()");
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            System.out.println(attributes);
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader("X-User");
                System.out.println(authorizationHeader);
                if (authorizationHeader != null) {
                    requestTemplate.header("X-User", authorizationHeader);
                }

            }
        };
    }

}
