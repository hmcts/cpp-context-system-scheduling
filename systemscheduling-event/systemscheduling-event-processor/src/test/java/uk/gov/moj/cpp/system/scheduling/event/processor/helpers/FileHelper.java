package uk.gov.moj.cpp.system.scheduling.event.processor.helpers;


import static javax.json.Json.createReader;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;

public class FileHelper {

    public static JsonObject readJsonObject(final String filePath, final Object... placeholders) {
        final String fileContent = readFile(filePath);
        final JsonReader jsonReader = createReader(new StringReader(String.format(fileContent, placeholders)));
        return jsonReader.readObject();
    }

    private static String readFile(final String path) {
        try {
            return Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
