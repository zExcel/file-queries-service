package com.walmart.service.function;

import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.TestTypes;
import com.walmart.service.models.Header;
import com.walmart.service.models.ListFilesRequest;
import com.walmart.service.models.ListFilesResponse;
import com.walmart.service.models.MultipleFilesResponse;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = LambdaConfigurationModule.class)
public class ListFilesTests extends AbstractLambdaTest {

    private String jpegFileId;
    private String pngFileId;
    private String pdfFileId;
    private String jpegCreationDate;
    private String pngCreationDate;

    public ListFilesTests() throws IOException {
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
        jpegCreationDate = response.getSuccessfulFiles().get(0).getCreationDate();
        pngFileId = response.getSuccessfulFiles().get(1).getFileUUID();
        pngCreationDate = response.getSuccessfulFiles().get(1).getCreationDate();
        pdfFileId = response.getSuccessfulFiles().get(2).getFileUUID();
    }

    @BeforeEach()
    void setupFiles() throws Exception {
        clearTableAndS3();
        uploadFiles();
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void listFilesByNameTest() throws Exception {

        // Should only return the pngFile and pdfFile.
        final ListFilesRequest fileNameQueryRequest = ListFilesRequest.builder()
                .nameBeginsWith("payload.p")
                .build();

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(post(format(LIST_FILES_FORMAT,
                                            TEST_USER_ID))
                                        .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .content(gson.toJson(fileNameQueryRequest)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final ListFilesResponse listFilesResponse = gson.fromJson(servletResponse.getContentAsString(), ListFilesResponse.class);
        final List<String> fileIds = listFilesResponse.getFileIDs();
        assertTrue(fileIds.containsAll(Arrays.asList(pngFileId, pdfFileId)));
        assertTrue(listFilesResponse.getNextToken() == null || listFilesResponse.getNextToken().isEmpty());
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void listFilesByTimeRangeTest() throws Exception {

        // This should only return the jpeg and png
        final ListFilesRequest fileNameQueryRequest = ListFilesRequest.builder()
                .createdBefore(pngCreationDate)
                .createdAfter(jpegCreationDate)
                .build();

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(post(format(LIST_FILES_FORMAT,
                                            TEST_USER_ID))
                                        .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .content(gson.toJson(fileNameQueryRequest)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final ListFilesResponse listFilesResponse = gson.fromJson(servletResponse.getContentAsString(), ListFilesResponse.class);
        final List<String> fileIds = listFilesResponse.getFileIDs();
        assertTrue(fileIds.containsAll(Arrays.asList(jpegFileId, pngFileId)));
        assertEquals(2, fileIds.size());
        assertTrue(listFilesResponse.getNextToken() == null || listFilesResponse.getNextToken().isEmpty());
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void listFilesByUserTest() throws Exception {
        // Blank creation parameters and nameBeginsWith should query by the user ID.
        final ListFilesRequest fileNameQueryRequest = ListFilesRequest.builder().build();

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(post(format(LIST_FILES_FORMAT,
                                            TEST_USER_ID))
                                        .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .content(gson.toJson(fileNameQueryRequest)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final ListFilesResponse listFilesResponse = gson.fromJson(servletResponse.getContentAsString(), ListFilesResponse.class);
        final List<String> fileIds = listFilesResponse.getFileIDs();
        assertTrue(fileIds.containsAll(Arrays.asList(jpegFileId, pngFileId, pdfFileId)));
        assertTrue(listFilesResponse.getNextToken() == null || listFilesResponse.getNextToken().isEmpty());
    }

}
