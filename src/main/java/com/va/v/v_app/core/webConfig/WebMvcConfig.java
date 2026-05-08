package com.va.v.v_app.core.webConfig;

import java.util.List;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.annotation.PostConstruct;
import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;

@Configuration
@RestController
public class WebMvcConfig implements WebMvcConfigurer {

	@Value("${cpDetails.crossOrigin.allowAll}")
	boolean CORS_allowAll;

	@Value("${cpDetails.crossOrigin.allowedIPs}")
	String[] allowedOrigins;
	// String[] allowedOrigins = {"http://localhost:3000"};
	// boolean CORS_allowAll = true;

	@Autowired
	MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;

	@PostConstruct
	public void onAfterLoad() {
		mappingJackson2HttpMessageConverter.getObjectMapper().setTimeZone(TimeZone.getDefault());
	}

	@Autowired
	RequestProcessingTimeInterceptor logInterceptor;

	@Autowired
	com.va.v.v_app.core.security.RateLimitingInterceptor rateLimitingInterceptor;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new SpecificationArgumentResolver());
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setPathMatcher(new AntPathMatcher());
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {

		if (!CORS_allowAll && allowedOrigins.length > 0) {
			System.out.println("Restricted Origins");
			registry.addMapping("/**")// .allowedOrigins(allowedOrigins)
					.allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE")
					.allowedOriginPatterns(allowedOrigins)
					.exposedHeaders("fileName").allowCredentials(true);
		} else {
			System.out.println("All allowed");
			registry.addMapping("/**")// .allowedOrigins("*")
					.allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE")
					.allowedOriginPatterns(allowedOrigins)
					.exposedHeaders("fileName").allowCredentials(true);
		}
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(logInterceptor);
		registry.addInterceptor(rateLimitingInterceptor);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
		// registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/pages/");
		resolver.setSuffix(".jsp");
		resolver.setViewClass(JstlView.class);
		registry.viewResolver(resolver);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.stream().forEach(con -> {
			if (con instanceof MappingJackson2HttpMessageConverter) {
				MappingJackson2HttpMessageConverter jk = (MappingJackson2HttpMessageConverter) con;
				jk.getObjectMapper().setTimeZone(TimeZone.getDefault());
				jk.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
			}
		});
	}
}
