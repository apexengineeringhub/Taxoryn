package com.taxoryn.module.itr.tax;

public enum AgeCategory {
    BELOW_60, SENIOR_CITIZEN, SUPER_SENIOR_CITIZEN;

    public static AgeCategory of(int age) {
        if (age >= 80) return SUPER_SENIOR_CITIZEN;
        if (age >= 60) return SENIOR_CITIZEN;
        return BELOW_60;
    }
}
