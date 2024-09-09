package com.example.demo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
    static final List<String> CUSTOMER_NAME_VARIANTS = Arrays.asList("customer names", "customer name", "cust name", "client name", "name");
    static final List<String> ENGINE_NUMBER_VARIANTS = Arrays.asList("engine numbers", "engine number", "eng no", "engine no", "engine", "eng", "enginenum");
    static final List<String> REGISTRATION_NUMBER_VARIANTS = Arrays.asList("registration numbers", "registration number", "reg no", "reg num", "reg", "registration", "regdnum");
    static final List<String> CHASSIS_NUMBER_VARIANTS = Arrays.asList("chasis number", "chasis no", "chassis number", "chassis", "chass no", "chass", "chassis no", "chasisnum");
    static final List<String> POSITION_VARIANTS = Arrays.asList("positions", "position", "pos", "po");
    static final List<String> MANDATORY_COLUMNS = new ArrayList<>(Arrays.asList( "registration number", "customer name", "engine number", "chassis number", "position"));
    static final int NUM_FIXED_HEADERS = MANDATORY_COLUMNS.size(); // +1 for the "Source File" column

    static final List<String[]> APPROVERS = Arrays.asList(
            new String[]{"Bablu", "7987059744"},
            new String[]{"Krishna Tiwari", "9993654016"},
            new String[]{"Santosh Tiwari", "9302465234"}
    );
}