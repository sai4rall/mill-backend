package com.millsystem.entity;

/** Quality grades, best first. C is the lowest grade that still passes; a sample below C fails and gets no grade. */
public enum QualityGrade {
    A_PLUS("A+"), A("A"), B_PLUS("B+"), B("B"), C("C");

    private final String label;

    QualityGrade(String label) { this.label = label; }

    public String label() { return label; }
}
