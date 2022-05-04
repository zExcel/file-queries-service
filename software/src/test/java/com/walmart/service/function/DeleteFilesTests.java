package com.walmart.service.function;

import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.TestTypes;
import com.walmart.service.models.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = LambdaConfigurationModule.class)
public class DeleteFilesTests extends AbstractLambdaTest {

    private String jpegFileId;
    private String pngFileId;
    private String pdfFileId;

    public DeleteFilesTests() throws IOException {
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
        pngFileId = response.getSuccessfulFiles().get(1).getFileUUID();
        pdfFileId = response.getSuccessfulFiles().get(2).getFileUUID();
    }

    @BeforeEach()
    void setupFiles() throws Exception {
        clearTableAndS3();
        uploadFiles();
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void deleteFileByFileIdTest() throws Exception {

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(delete(format(DELETE_FILE_BY_ID_FORMAT,
                                              TEST_USER_ID,
                                              jpegFileId)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final File deletedFile = gson.fromJson(servletResponse.getContentAsString(), File.class);
        assertEquals(deletedFile.getOwnerID(), TEST_USER_ID);
        assertEquals(deletedFile.getFileUUID(), (jpegFileId));
        assertEquals(0, s3Client.listObjects(ListObjectsRequest.builder()
                                                     .bucket(configurationModule.getBucketName())
                                                     .prefix(String.format("%s/%s", TEST_USER_ID, JPEG_PAYLOAD_FILE_NAME))
                                                     .build()).contents().size());
        assertTrue(dynamoDbClient.getItem(GetItemRequest.builder()
                                                  .key(Collections.singletonMap(TableAttributes.FILE_ID_KEY,
                                                                                AttributeValue.builder().s(jpegFileId).build()))
                                                  .tableName(configurationModule.getTableName())
                                                  .build()).item().isEmpty());
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void deleteFileByFileIdWrongUserTest() {
        final String wrongUser = "wrong_user";

        assertThrows(NestedServletException.class, () -> mockMvc.perform(delete(format(DELETE_FILE_BY_ID_FORMAT,
                                                                                       wrongUser,
                                                                                       jpegFileId))));
    }

    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void deleteMultipleFilesTest() throws Exception {
        final DeleteFilesRequest deleteFilesRequest = DeleteFilesRequest.builder()
                .fileNames(Arrays.asList(JPEG_PAYLOAD_FILE_NAME, PNG_PAYLOAD_FILE_NAME))
                .fileIds(Collections.singletonList(pdfFileId))
                .build();

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(post(format(DELETE_MULTIPLE_FILES_FORMAT,
                                            TEST_USER_ID))
                                        .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .content(gson.toJson(deleteFilesRequest)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final MultipleFilesResponse deleteFilesResponse = gson.fromJson(servletResponse.getContentAsString(), MultipleFilesResponse.class);
        final List<File> successfulDeletions = deleteFilesResponse.getSuccessfulFiles();
        final List<Pair> failedDeletions = deleteFilesResponse.getFailedFiles();
        final List<String> deletedFileIds = successfulDeletions.stream().map(File::getFileUUID).collect(Collectors.toList());

        assertThrows(NoSuchKeyException.class, () ->
                s3Client.getObject(GetObjectRequest.builder()
                                           .key(jpegFileId)
                                           .bucket(configurationModule.getBucketName())
                                           .build()));
        assertThrows(NoSuchKeyException.class, () ->
                s3Client.getObject(GetObjectRequest.builder()
                                           .key(pdfFileId)
                                           .bucket(configurationModule.getBucketName())
                                           .build()));
        assertTrue(deletedFileIds.containsAll(Arrays.asList(jpegFileId, pngFileId, pdfFileId)));
        assertTrue(failedDeletions == null || failedDeletions.size() == 0);
    }


    @Test
    @Tag(TestTypes.INTEGRATION_TEST)
    void deleteMultipleFilesFailedDeletionsTest() throws Exception {
        final MvcResult uploadResult = mockMvc.perform(multipart(format(UPLOAD_FILES_FORMAT,
                                                                        "unknown_user"))
                                                               .file(jpegPayloadFile))
                .andExpect(status().is(200))
                .andReturn();
        final MultipleFilesResponse response = gson.fromJson(uploadResult.getResponse().getContentAsString(), MultipleFilesResponse.class);
        final String differentUsersFileId = response.getSuccessfulFiles().get(0).getFileUUID();


        final String garbageFileName = "garbage_file_name.png";
        final DeleteFilesRequest deleteFilesRequest = DeleteFilesRequest.builder()
                .fileNames(Arrays.asList(garbageFileName, PNG_PAYLOAD_FILE_NAME))
                .fileIds(Collections.singletonList(differentUsersFileId))
                .build();

        final MockHttpServletResponse servletResponse =
                mockMvc.perform(post(format(DELETE_MULTIPLE_FILES_FORMAT,
                                            TEST_USER_ID))
                                        .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .content(gson.toJson(deleteFilesRequest)))
                        .andExpect(status().is(200))
                        .andReturn()
                        .getResponse();

        final MultipleFilesResponse deleteFilesResponse = gson.fromJson(servletResponse.getContentAsString(), MultipleFilesResponse.class);
        final List<File> successfulDeletions = deleteFilesResponse.getSuccessfulFiles();
        final List<Pair> failedDeletions = deleteFilesResponse.getFailedFiles();
        final List<String> deletedFileIds = successfulDeletions.stream().map(File::getFileUUID).collect(Collectors.toList());

        assertThrows(NoSuchKeyException.class, () ->
                s3Client.getObject(GetObjectRequest.builder()
                                           .key(pngFileId)
                                           .bucket(configurationModule.getBucketName())
                                           .build()));
        assertEquals(1, deletedFileIds.size());
        assertEquals(pngFileId, deletedFileIds.get(0));
        assertEquals(2, failedDeletions.size());
        for (Pair error : failedDeletions) {
            if (error.getLeft().equals(garbageFileName)) {
                assertEquals(HttpServletResponse.SC_BAD_REQUEST, error.getRight());
            } else {
                assertEquals(HttpServletResponse.SC_UNAUTHORIZED, error.getRight());
            }
        }
    }
}
