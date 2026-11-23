package com.edupulse.ingeststatic.pdf;

/**
 * Raw text extracted from one PDF page.
 *
 * @param pageNumber 1-based page number
 * @param text       extracted text; paragraphs are separated by a blank line
 */
public record PageText(int pageNumber, String text) {
}
