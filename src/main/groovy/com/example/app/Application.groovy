package com.example.app

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import static org.springframework.web.reactive.function.server.RequestPredicates.GET
import static org.springframework.web.reactive.function.server.RouterFunctions.route
import static org.springframework.web.reactive.function.server.ServerResponse.ok

@SpringBootApplication
class Application {

  @Autowired Config config

  static void main(String[] args) {
    SpringApplication.run Application, args
  }

  @Bean
  def routes() {
    route(GET("/"), req -> ok().render("greeting", config.properties))
  }

  @Configuration
  @ConfigurationProperties(prefix = "app")
  static class Config {
    String name
  }

}
