package models;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

public class RequestTests {
    private final String listFilesJson = "{\"name\": \"testing\"}";


    @Test
    public void listFilesDeserializesProperly() {
        final Gson gson = new Gson();
        ListFilesRequest listFilesRequest = ListFilesRequest.requestFromJson(listFilesJson, gson);

        assert(listFilesRequest.getName() != null);
        assert(listFilesRequest.getName().equals("testing"));
        assert(listFilesRequest.getNameContains() == null);
    }
}
