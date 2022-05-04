package com.walmart.service.models;

import com.walmart.service.LambdaApplication;
import com.walmart.service.TestTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = LambdaApplication.class)
public class FileTypeTests {
    private static final Logger logger = LoggerFactory.getLogger(FileTypeTests.class);
    private final String pngFileType = "png";
    private final String pdfFileType = "pdf";

    @Test
    @Tag(TestTypes.UNIT_TEST)
    void fileTypeExistsWorks() {
        assert FileType.fileTypeExists(pngFileType);
        assert FileType.fileTypeExists(pdfFileType);
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    void fromStringWorks() {
        assert FileType.fromString(pngFileType) == FileType.PNG;
        assert FileType.fromString(pdfFileType) == FileType.PDF;
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    void getMediaTypeWorks() {
        assert FileType.getMediaType(pdfFileType).equals("application");
        assert FileType.getMediaType(pngFileType).equals("image");
    }
}