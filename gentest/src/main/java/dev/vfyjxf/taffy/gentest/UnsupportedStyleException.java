package dev.vfyjxf.taffy.gentest;

/** Raised when a fixture uses a CSS feature the generator cannot represent. */
final class UnsupportedStyleException extends RuntimeException {
    UnsupportedStyleException(String message) {
        super(message);
    }
}
