package com.csfrez.demospringboot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/","/home","/actuator/**","/country/**").permitAll()
				.anyRequest().authenticated().and()
			.formLogin()
				.usernameParameter("username")
				.passwordParameter("password")
//				.failureForwardUrl("/login?error")
				.loginPage("/login").permitAll().and()
			.logout().permitAll();
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/country/**");
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		//auth.inMemoryAuthentication().withUser("admin").password("admin").roles("USER");
	    //inMemoryAuthentication 从内存中获取  
	    //auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder()).withUser("admin").password(new BCryptPasswordEncoder().encode("123456")).roles("USER");
	}
	
	@Bean
    @Override
    public UserDetailsService userDetailsService() {
        @SuppressWarnings("deprecation")
		UserDetails user = User.withDefaultPasswordEncoder().username("admin").password("654321").roles("USER").build();
        return new InMemoryUserDetailsManager(user);
    }
}
