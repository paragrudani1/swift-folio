package io.swiftfolio.core

/**
 * Reasons an input field is invalid. The UI maps each value to a localized message;
 * validators never produce user-facing text themselves.
 */
enum class FieldError {
    EMPTY,
    TOO_SHORT,
    TOO_LONG,
    INVALID_CHARACTERS,
    UPPERCASE,
    BAD_START_OR_END,
    CONSECUTIVE_DOTS,
    IP_ADDRESS_FORMAT,
    RESERVED_NAME,
    CONTAINS_SLASH,
    DOT_SEGMENT,
    ALREADY_EXISTS,
    KEY_TOO_LONG,
}
