package com.walmart.service.function;

import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.TestTypes;
import com.walmart.service.models.File;
import com.walmart.service.models.UploadFilesResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = LambdaConfigurationModule.class)
public class UploadFileTests extends AbstractLambdaTest {

    public UploadFileTests() throws IOException {
        super();
    }

    private boolean fileIsValid(final File file, final String fileName, final String userId) {
        return file.getFileName().equals(fileName) &&
                file.getOwnerID().equals(userId) &&
                !file.getFileUUID().isEmpty() &&
                !file.getCreationDate().isEmpty();
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void uploadFileWorks() throws Exception {

        final MvcResult uploadJpegResult = mockMvc.perform(multipart(format(UPLOAD_FILES_FORMAT,
                                                                            TEST_USER_ID))
                                                                   .file(jpegPayloadFile))
                .andExpect(status().is(200))
                .andReturn();
        final UploadFilesResponse uploadJpegFile = gson.fromJson(uploadJpegResult.getResponse().getContentAsString(), UploadFilesResponse.class);
        assert fileIsValid(uploadJpegFile.getSuccessfulFiles().get(0), JPEG_PAYLOAD_FILE_NAME, TEST_USER_ID);
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void uploadMultipleFileWorks() throws Exception {

        final MvcResult uploadJpegResult = mockMvc.perform(multipart(format(UPLOAD_FILES_FORMAT,
                                                                            TEST_USER_ID))
                                                                   .file(jpegPayloadFile)
                                                                   .file(pngPayloadFile)
                                                                   .file(pdfPayloadFile))
                .andExpect(status().is(200))
                .andReturn();
        final UploadFilesResponse uploadFilesResponse = gson.fromJson(uploadJpegResult.getResponse().getContentAsString(), UploadFilesResponse.class);
        final List<File> successfulUploads = uploadFilesResponse.getSuccessfulFiles();
        final List<String> failedUploads = uploadFilesResponse.getFailedFiles();

        assert failedUploads.isEmpty();
        assert successfulUploads.size() == 3;

        final File jpegFile = successfulUploads.get(0);
        final File pngFile = successfulUploads.get(1);
        final File pdfFile = successfulUploads.get(2);
        assert fileIsValid(jpegFile, JPEG_PAYLOAD_FILE_NAME, TEST_USER_ID);
        assert fileIsValid(pngFile, PNG_PAYLOAD_FILE_NAME, TEST_USER_ID);
        assert fileIsValid(pdfFile, PDF_PAYLOAD_FILE_NAME, TEST_USER_ID);
    }
}
