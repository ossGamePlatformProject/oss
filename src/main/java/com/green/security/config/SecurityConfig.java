package com.green.security.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.NimbusAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;

import com.green.oauth2.service.CustomOAuth2UserService;
import com.green.oauth2.service.CustomOidcUserService;
import com.green.security.service.CustomUserDetailsService;
import com.green.security.utils.CustomLoginSuccessHandler;

import lombok.Setter;

@PropertySource("classpath:/application.properties")
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Setter(onMethod_=@Autowired)
	private Environment env;
	
	@Autowired
	private DataSource dataSource;
	
	private static List<String> clients = Arrays.asList("google", "facebook");
	//application.properties ??? ?????? ???????????? ??????
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		
		http	.csrf().disable()
		.authorizeRequests()
		.antMatchers("/game/register", "/game/modify**", "/news/register", "/news/modify**").hasRole("ADMIN")
		//?????? ????????? ?????? ?????? ??????
		.antMatchers("/oauth_login", "/login/oauth2/code/**").permitAll()
		//????????? ???????????? ?????? ??????
		.anyRequest().permitAll();
			
		http
		.formLogin()
		.loginPage("/user/login")
		.loginProcessingUrl("/processingLogin")
		.successHandler(authenticationSuccessHandler())
		.failureUrl("/user/login?error=error")
		.and().userDetailsService(userDetailsService());
		
		http
		.oauth2Login()
		
			.loginPage("/")
			.defaultSuccessUrl("/", true)
			// client id, client secret, redirect uri  ??? ??? ??????
			.clientRegistrationRepository(clientRegistrationRepository())
			//loadAuthorizedClient() ??? ??????????????? ????????? ????????? ??? ??????
			.authorizedClientService(authorizedClientService())
			//????????? ???????????? ??????
			.tokenEndpoint()
			//redirect uri ??? ??????????????? ?????? authorization ????????? ????????? ??????(authorization sever) ?????? accessToken ?????? ????????????
			.accessTokenResponseClient(accessTokenResponseClient())
			.and()
			//Resource owner??? ????????? Client??? ???????????? ?????? ?????????
			.authorizationEndpoint()
			.baseUri("/oauth2/authorization").and()
			//?????? ????????? ???????????? ?????? ??????. Authorization Request ??? ??????
//			.authorizationRequestRepository(authorizationRequestRepository()).and()
//			
			//userInfoEndpoint ??? accessToken ??? ????????? ????????? ????????? ???????????? ???????????? url 
			.userInfoEndpoint()
			.userService(userService())
			.oidcUserService(oidcUserService());
		
		http.rememberMe().userDetailsService(userDetailsService())
		.tokenRepository(persistentTokenRepository());
		
		http
		.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/")
			.invalidateHttpSession(true)
			//for spring security
			.clearAuthentication(true)
			
			.deleteCookies("JSESSIONID");
		
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		// TODO Auto-generated method stub
		web.ignoring()
		.antMatchers("/resources/**")
		.antMatchers("/css/**").antMatchers("/js/**").antMatchers("/img/**").antMatchers("/vendor/**");
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		// TODO Auto-generated method stub
		auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
	}

	
	
	//Resource Server??? ???????????? client ????????? ??????
	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		
		List<ClientRegistration> registrations = clients.stream()
				//client id??? secret??? ?????? CommonOAuth2Provider ????????? ??????
				.map(c -> getRegistration(c))
				.filter(registration -> registration != null)
				.collect(Collectors.toList());
		
		registrations.forEach(i -> System.out.println(i.getClientId()));

		//Client ????????? ??????
		return new InMemoryClientRegistrationRepository(registrations);
	} 
	
	private ClientRegistration getRegistration(String client) {
		String clientId = env.getProperty(
				//Environment??? properties????????? ?????????
				CLIENT_PROPERTY_KEY + client + ".client-id");
		
		
		
		if(clientId == null) {
			return null;
		}
		
		String clientSecret = env.getProperty(
				CLIENT_PROPERTY_KEY + client + ".client-secret");
		
		System.out.println("=======================================");
		System.out.println("clientId".toString() + clientId);
		System.out.println("=======================================");
		//CommonOAuth2Provider?????? enum ????????? Google ,Facebook, Github , Okta ??? ??????.
		//redirect uri ?????? ???????????????.
		if (client.equals("google")) {
			return CommonOAuth2Provider.GOOGLE.getBuilder(client)
					.clientId(clientId).clientSecret(clientSecret).build();
		}
		
		if(client.equals("facebook")) {
			return CommonOAuth2Provider.FACEBOOK.getBuilder(client)
					.clientId(clientId).clientSecret(clientSecret).build();
		}
		
		return null;
	}
	
	@Bean
	public OAuth2AuthorizedClientService authorizedClientService() {
		return new InMemoryOAuth2AuthorizedClientService(
				clientRegistrationRepository());
	}
	
	@Bean
	public AuthorizationRequestRepository<OAuth2AuthorizationRequest>
		authorizationRequestRepository(){
		
		return new HttpSessionOAuth2AuthorizationRequestRepository();
	}
	
	@Bean
	public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
		accessTokenResponseClient(){
		return new NimbusAuthorizationCodeTokenResponseClient();
	}
	
	@Bean(name = BeanIds.AUTHENTICATION_MANAGER)
   @Override
   public AuthenticationManager authenticationManagerBean() throws Exception {
       return super.authenticationManagerBean();
   }
	
	//UserInfo EndPoint (???????????? ????????? ????????? ?????? ?????????) ??? ??????
	@Bean
	public OAuth2UserService userService() {
		return new CustomOAuth2UserService();
	}
	//google ??? user service
	@Bean
	public OidcUserService oidcUserService() {
		return new CustomOidcUserService();
	}

	//?????? ???????????? ?????? bean???
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public UserDetailsService userDetailsService() {
		return new CustomUserDetailsService();
	}
	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
		return new CustomLoginSuccessHandler();
	}
	
	@Bean
	public PersistentTokenRepository persistentTokenRepository() {
		JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
		jdbcTokenRepository.setDataSource(dataSource);
		return jdbcTokenRepository;
	}
	
}
