package vn.naitei.nhom3.expensemanagement.importexport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import vn.naitei.nhom3.expensemanagement.entity.Category;
import vn.naitei.nhom3.expensemanagement.entity.User;
import vn.naitei.nhom3.expensemanagement.entity.enums.CategoryType;
import vn.naitei.nhom3.expensemanagement.entity.enums.Role;
import vn.naitei.nhom3.expensemanagement.entity.enums.UserStatus;
import vn.naitei.nhom3.expensemanagement.repository.CategoryRepository;
import vn.naitei.nhom3.expensemanagement.repository.UserRepository;
import vn.naitei.nhom3.expensemanagement.security.JwtTokenProvider;
import vn.naitei.nhom3.expensemanagement.security.UserPrincipal;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ImportExportAdminIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient restClient;
    private User admin;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
        uniqueSuffix = UUID.randomUUID().toString();
        admin = saveUser("importexport-admin-" + uniqueSuffix, Role.ADMIN);
        regularUser = saveUser("importexport-user-" + uniqueSuffix, Role.USER);
        adminToken = jwtTokenProvider.generateToken(new UserPrincipal(admin));
        userToken = jwtTokenProvider.generateToken(new UserPrincipal(regularUser));
    }

    @AfterEach
    void tearDown() {
        categoryRepository.findByUserIsNullAndNameAndTypeAndDeletedAtIsNull(
                "Cat-" + uniqueSuffix, CategoryType.EXPENSE).ifPresent(categoryRepository::delete);
        categoryRepository.findByUserIsNullAndNameAndTypeAndDeletedAtIsNull(
                "Cat-" + uniqueSuffix + "-2", CategoryType.EXPENSE).ifPresent(categoryRepository::delete);
        userRepository.deleteById(admin.getId());
        userRepository.deleteById(regularUser.getId());
    }

    @Test
    void shouldExportCategoriesAsCsvForAdmin() throws Exception {
        Category category = saveCategory("Cat-" + uniqueSuffix, CategoryType.EXPENSE);

        ResponseEntity<byte[]> response = restClient.get()
                .uri("/api/admin/export/category")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .retrieve()
                .toEntity(byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("category.csv");

        CSVRecord row = findRowByColumn(response.getBody(), "name", category.getName());
        assertThat(row.get("type")).isEqualTo("EXPENSE");
        assertThat(row.get("scope")).isEqualTo("COMMON");

        categoryRepository.delete(category);
    }

    @Test
    void shouldExportUsersAsCsvForAdmin() throws Exception {
        ResponseEntity<byte[]> response = restClient.get()
                .uri("/api/admin/export/user")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .retrieve()
                .toEntity(byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("user.csv");

        CSVRecord row = findRowByColumn(response.getBody(), "email", admin.getEmail());
        assertThat(row.get("totalExpense")).isEqualTo("0.00");
        assertThat(row.get("totalIncome")).isEqualTo("0.00");
        assertThat(row.isSet("password")).isFalse();
    }

    @Test
    void shouldRejectExportWithInvalidEntity() {
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/admin/export/foo")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void shouldRejectExportWithUnsupportedFormat() {
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/admin/export/category?format=xlsx")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void shouldForbidNonAdminFromExportingAndImporting() {
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/admin/export/category")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Forbidden.class);

        assertThatThrownBy(() -> importCsv(userToken, "category", "name,description,type\nX,Y,EXPENSE\n"))
                .isInstanceOf(HttpClientErrorException.Forbidden.class);
    }

    @Test
    void shouldRejectImportWithInvalidEntity() {
        assertThatThrownBy(() -> importCsv(adminToken, "foo", "a,b\n1,2\n"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void shouldImportCategoriesAndReportPartialFailures() throws Exception {
        String csv = "name,description,type\n"
                + "Cat-" + uniqueSuffix + ",Mo ta hop le,EXPENSE\n"
                + "Cat-" + uniqueSuffix + "-2,Mo ta khong hop le,INVALID_TYPE\n";

        JsonNode response = importCsv(adminToken, "category", csv);

        assertThat(response.at("/data/successCount").asInt()).isEqualTo(1);
        assertThat(response.at("/data/failedCount").asInt()).isEqualTo(1);
        assertThat(response.at("/data/errors/0").asText()).contains("Dòng 3");

        assertThat(categoryRepository.findByUserIsNullAndNameAndTypeAndDeletedAtIsNull(
                "Cat-" + uniqueSuffix, CategoryType.EXPENSE)).isPresent();
        assertThat(categoryRepository.findByUserIsNullAndNameAndTypeAndDeletedAtIsNull(
                "Cat-" + uniqueSuffix + "-2", CategoryType.EXPENSE)).isEmpty();
    }

    private JsonNode importCsv(String token, String entity, String csvContent) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return entity + ".csv";
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType("text/csv"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, fileHeaders));

        String response = restClient.post()
                .uri("/api/admin/import/{entity}", entity)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(response);
    }

    private CSVRecord findRowByColumn(byte[] csvBytes, String column, String value) throws Exception {
        String content = stripBom(csvBytes);
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
            List<CSVRecord> records = parser.getRecords();
            return records.stream()
                    .filter(record -> value.equals(record.get(column)))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Không tìm thấy dòng CSV với " + column + "=" + value));
        }
    }

    private String stripBom(byte[] bytes) {
        int offset = bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF
                ? 3 : 0;
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
    }

    private Category saveCategory(String name, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setDescription("Mo ta");
        category.setType(type);
        return categoryRepository.saveAndFlush(category);
    }

    private User saveUser(String prefix, Role role) {
        User user = new User();
        user.setName(prefix);
        user.setEmail(prefix + "@test.com");
        user.setPassword("test-password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
