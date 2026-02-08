package ke.co.interviewusercaseworld.productconfig.configs;

import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ProductConfigsProperties properties;
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        Helpers.log(null, LogLevelEnum.info, OperationNameEnum.JWT_PROCESSING, "Converting jwt to auth token", null);
        Collection<GrantedAuthority> authorities = Stream.concat(jwtGrantedAuthoritiesConverter.convert(source).stream(),
                        extractedResourceRoles(source).stream())
                .collect(Collectors.toSet());
        return new JwtAuthenticationToken(source, authorities, getPrincipalClaimName(source));
    }

    private String getPrincipalClaimName(Jwt jwt) {
        Helpers.log(null, LogLevelEnum.info, OperationNameEnum.JWT_PROCESSING, "Extracting the principal", null);
        String principalAttribute = properties.getServiceProperties().getProductConfiguration().getSecurityConfigSpec().getJwtSpec().getPrincipalAttribute();
        String principal = (String) jwt.getClaims().get(principalAttribute);
        if (principal == null || principal.isEmpty() || principal.isBlank()) {
            Helpers.log(null, LogLevelEnum.warn, OperationNameEnum.JWT_PROCESSING, "Principal not found", null);
            return "anonymous";
        } else Helpers.log(null, LogLevelEnum.info, OperationNameEnum.JWT_PROCESSING, "Principal found", null);
        return principal;
    }

    private Collection<? extends GrantedAuthority> extractedResourceRoles(Jwt jwt) {
        Helpers.log(null, LogLevelEnum.info, OperationNameEnum.JWT_PROCESSING, "Extracting resource roles", null);
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Map<String, Object> resource;
        Collection<String> resourceRoles;

        resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            Helpers.log(null, LogLevelEnum.error, OperationNameEnum.JWT_PROCESSING, "Resource access not set", new RuntimeException("Resource access not set in the token"));
            return Set.of();
        }

        String resourceId = properties.getServiceProperties().getProductConfiguration().getSecurityConfigSpec().getJwtSpec().getResourceId();
        resource = (Map<String, Object>) resourceAccess.get(resourceId);
        if (resource == null) {
            Helpers.log(null, LogLevelEnum.error, OperationNameEnum.JWT_PROCESSING, "Resource " + resourceId + " not found", null);
            return Set.of();
        }

        resourceRoles = (Collection<String>) resource.get("roles");
        if (resourceRoles == null) {
            Helpers.log(null, LogLevelEnum.error, OperationNameEnum.JWT_PROCESSING, "Roles for the resource " + resourceId + " not found", null);
            return Set.of();
        }

        return resourceRoles.stream()
                .map(role -> {
                    Helpers.log(null, LogLevelEnum.debug, OperationNameEnum.JWT_PROCESSING, "ROLE_" + role, null);
                    return new SimpleGrantedAuthority("ROLE_" + role);
                })
                .collect(Collectors.toSet());
    }

}
