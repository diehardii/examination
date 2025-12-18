package seucxxy.csd.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
    "seucxxy.csd.backend.common.mapper",
    "seucxxy.csd.backend.cet4.mapper",
    "seucxxy.csd.backend.hs3.mapper"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
