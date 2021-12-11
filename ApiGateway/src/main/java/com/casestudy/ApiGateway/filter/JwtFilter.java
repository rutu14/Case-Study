package com.casestudy.ApiGateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.casestudy.ApiGateway.dto.UserDTO;
import com.casestudy.ApiGateway.util.JwtUtil;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

	@Autowired
    private JwtUtil jwtUtil;
	
	private final WebClient.Builder webClientBuilder;
	
	public JwtFilter(WebClient.Builder webClientBuilder ) {
		super(Config.class);
        this.webClientBuilder =  webClientBuilder;
    }
	
	private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0);
    }
	
	private boolean isAuthMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus)  {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
    
    private void populateRequestWithHeaders(ServerWebExchange exchange, String token) {
        Claims claims = jwtUtil.getAllClaimsFromToken(token);
        exchange.getRequest().mutate()
                .header("userid", String.valueOf(claims.get("sub")))
                .header("role", String.valueOf(claims.get("aud")))
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (isAuthMissing(request))
            return onError(exchange, "Authorization header is missing in the request", HttpStatus.UNAUTHORIZED);

            String token = getAuthHeader(request); 
               if (jwtUtil.isInvalid(token))
                    return onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);

	        if (!request.getHeaders().containsKey("Authorization")) {
	        	return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
	        };
	
            return webClientBuilder.build()
            					   .post()
            					   .uri( "http://ACCOUNTS/account/validateJWT?token=" + token )
            					   .retrieve().bodyToMono( UserDTO.class )
            					   .map( userDto -> {
            						   exchange.getRequest().mutate().header("x-auth-user-id",String.valueOf( userDto.getId())); 
            						   populateRequestWithHeaders(exchange, token);
            						   exchange.getResponse().getHeaders().set(HttpHeaders.AUTHORIZATION, userDto.getToken());
            						   return exchange;})
            					   .flatMap( chain:: filter);
        };
    }

    public static class Config {}


}
