package com.walmart.service.function;

import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.TestTypes;
import com.walmart.service.models.MultipleFilesResponse;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = LambdaConfigurationModule.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetFileTests extends AbstractLambdaTest {

    private String jpegFileId;

    public GetFileTests() throws IOException {
        super();
    }

    private void uploadFiles() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(multipart(format(UPLOAD_FILES_FORMAT,
                                                                     TEST_USER_ID))
                                                            .file(jpegPayloadFile)
                                                            .file(pngPayloadFile)
                                                            .file(pdfPayloadFile))
                .andExpect(status().is(200))
                .andReturn();
        final MultipleFilesResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), MultipleFilesResponse.class);
        jpegFileId = response.getSuccessfulFiles().get(0).getFileUUID();
    }

    @BeforeAll
    void setupFiles() throws Exception {
        uploadFiles();
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void getFileByNameWorks() throws Exception {

        final MockHttpServletResponse getJpegResult = mockMvc.perform(get(format(GET_FILE_BY_NAME_FORMAT,
                                                                                 TEST_USER_ID,
                                                                                 JPEG_PAYLOAD_FILE_NAME)))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse();

        assertEquals("image/jpeg", getJpegResult.getContentType());
        assertEquals(getJpegResult.getContentLength(), jpegPayloadFile.getSize());
        Assertions.assertArrayEquals(jpegPayloadFile.getBytes(), getJpegResult.getContentAsByteArray());
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void getFileByIdWorks() throws Exception {

        final MockHttpServletResponse getJpegResult = mockMvc.perform(get(format(GET_FILE_BY_ID_FORMAT,
                                                                                 jpegFileId)))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse();

        assertEquals("image/jpeg", getJpegResult.getContentType());
        assertEquals(getJpegResult.getContentLength(), jpegPayloadFile.getSize());
        Assertions.assertArrayEquals(jpegPayloadFile.getBytes(), getJpegResult.getContentAsByteArray());
    }

}
