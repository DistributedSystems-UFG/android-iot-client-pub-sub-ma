package com.example.thermoledmobileclient.models;

public class SessaoClient {
    private static String token;
    private static String funcionalidade;

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SessaoClient.token = token;
    }

    public static String getFuncionalidade() {
        return funcionalidade;
    }

    public static void setFuncionalidade(String funcionalidade) {
        SessaoClient.funcionalidade = funcionalidade;
    }
}


