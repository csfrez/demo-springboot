package com.csfrez.demospringboot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
//		http.authorizeRequests()
//				.antMatchers("/","/home","/actuator/**","/country/**","/druid/**").permitAll()
//				.anyRequest().authenticated();
		http.formLogin()                    //  定义当需要用户登录时候，转到的登录页面。
         .and()
         .authorizeRequests().antMatchers("/","/sayHello","/actuator/**").permitAll()        // 定义哪些URL需要被保护、哪些不需要被保护
         .anyRequest()               // 任何请求,登录后可以访问
         .authenticated();
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		//web.ignoring().antMatchers("/country/**","/druid/**");
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		//auth.inMemoryAuthentication().withUser("admin").password("admin").roles("USER");
	    //inMemoryAuthentication 从内存中获取  
	    //auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder()).withUser("admin").password(new BCryptPasswordEncoder().encode("123456")).roles("USER");
	}
	/*
	@Bean
    @Override
    public UserDetailsService userDetailsService() {
        @SuppressWarnings("deprecation")
		UserDetails user = User.withDefaultPasswordEncoder().username("admin").password("654321").roles("USER").build();
        return new InMemoryUserDetailsManager(user);
    }
	*/
}
