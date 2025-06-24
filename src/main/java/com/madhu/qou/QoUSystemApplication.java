package com.madhu.qou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import com.madhu.qou.service.DataSeeder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info = @Info(
        title = "QoU Search API",
        version = "v1",
        description = "Real-time Query Understanding search endpoints"
    )
)
@SpringBootApplication
public class QoUSystemApplication {

   public static void main(String[] args) {
       SpringApplication.run(QoUSystemApplication.class, args);
   }

   /**
    * On application startup, seed Elasticsearch indices if they do not already exist.
    */
   @Bean
   public CommandLineRunner seedDataRunner(DataSeeder dataSeeder) {
       return args -> {
           dataSeeder.seedProductIndex();
           dataSeeder.seedSuggestionIndex();
       };
   }

}
