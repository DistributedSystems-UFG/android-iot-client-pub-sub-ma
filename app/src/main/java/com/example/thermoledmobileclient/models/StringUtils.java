package com.example.thermoledmobileclient.models;

public class StringUtils {

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
    //deixa a primeira letra maiúscula
    public static String capitalizeEachWord(String input) {

        String[] words = input.split(" ");
        StringBuilder output = new StringBuilder();
        for (String word : words) {
            output.append(word.substring(0, 1).toUpperCase());
            output.append(word.substring(1).toLowerCase());
            output.append(" ");
        }
        String result = output.toString().trim();
        System.out.println(result);

        return result;
    }
}
