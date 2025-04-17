package com.smart.quiz;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizApplication {

	public static void main(String[] args) {
		// 1. ArrayList orqali List ishlatish
		List<String> list1 = new ArrayList<>();
		list1.add("Java");
		list1.add("Python");
		out.println("ArrayList: " + list1);

		// 2. LinkedList orqali List ishlatish
		List<String> list2 = new LinkedList<>();
		list2.add("C++");
		list2.add("JavaScript");
		out.println("LinkedList: " + list2);

		SpringApplication.run(QuizApplication.class, args);
	}

}
