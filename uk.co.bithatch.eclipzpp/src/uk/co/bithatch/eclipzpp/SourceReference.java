package uk.co.bithatch.eclipzpp;

public record SourceReference(SourceReferenceType type, int line, int originalLine, String originalUri) {}