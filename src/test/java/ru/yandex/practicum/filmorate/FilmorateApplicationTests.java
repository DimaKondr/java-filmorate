package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = FilmorateApplication.class)
class FilmorateApplicationTests {

   	@Test
	void contextLoads(ApplicationContext context) {
        assertNotNull(context, "Контекст не создан");
    }

}