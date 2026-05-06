package com.example.mely.stocks;

import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.Membership;
import com.example.mely.model.Organization;
import com.example.mely.model.User;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.OrganizationRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.security.jwt.JwtUtils;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import com.example.mely.stocks.repository.StockScreeningDocumentRepository;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;
import com.example.mely.stocks.repository.StockScreeningResultRepository;
import com.example.mely.stocks.service.FinancialMetricsExtractor;
import com.example.mely.stocks.service.analysis.ExtractedMetric;
import com.example.mely.stocks.service.analysis.ExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StockScreeningControllerTest {

    private static final Path STORAGE_PATH;

    static {
        try {
            STORAGE_PATH = Files.createTempDirectory("mely-stocks-test");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("mely.stocks.storage.base-path", () -> STORAGE_PATH.toString());
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private StockScreeningRequestRepository stockScreeningRequestRepository;

    @Autowired
    private StockScreeningResultRepository stockScreeningResultRepository;

    @Autowired
    private StockScreeningDocumentRepository stockScreeningDocumentRepository;

    private Organization organization;
    private String authHeader;
    private String saasAuthHeader;
    private String secondSaasAuthHeader;
    private String loanOfficerAuthHeader;
    private User saasUser;
    private User secondSaasUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        organization = organizationRepository.save(Organization.builder()
                .name("Test Bank")
                .slug("test-bank")
                .build());

        User user = User.builder()
                .firstName("Test")
                .lastName("Admin")
                .email("stocks@test-bank.tn")
                .password(passwordEncoder.encode("Password123!"))
                .roleType(RoleType.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Membership membership = Membership.builder()
                .user(user)
                .role(RoleType.ADMIN)
                .build();
        membership.setOrganizationId(organization.getId());
        membership.setCreatedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        authHeader = "Bearer " + jwtUtils.generateJwtToken(user.getId(), user.getEmail(), RoleType.ADMIN.name(), organization.getId(), "BANK");

        User loanOfficer = User.builder()
                .firstName("Loan")
                .lastName("Officer")
                .email("loan.officer@test-bank.tn")
                .password(passwordEncoder.encode("Password123!"))
                .roleType(RoleType.LOAN_OFFICER)
                .createdAt(LocalDateTime.now())
                .build();
        loanOfficer = userRepository.save(loanOfficer);

        Membership loanOfficerMembership = Membership.builder()
                .user(loanOfficer)
                .role(RoleType.LOAN_OFFICER)
                .build();
        loanOfficerMembership.setOrganizationId(organization.getId());
        loanOfficerMembership.setCreatedAt(LocalDateTime.now());
        membershipRepository.save(loanOfficerMembership);

        loanOfficerAuthHeader = "Bearer " + jwtUtils.generateJwtToken(
                loanOfficer.getId(),
                loanOfficer.getEmail(),
                RoleType.LOAN_OFFICER.name(),
                organization.getId(),
                "BANK"
        );

        saasUser = User.builder()
                .firstName("SaaS")
                .lastName("User")
                .email("saas.user@mely.app")
                .password(passwordEncoder.encode("Password123!"))
                .roleType(RoleType.SAAS_USER)
                .createdAt(LocalDateTime.now())
                .build();
        saasUser = userRepository.save(saasUser);
        saasAuthHeader = "Bearer " + jwtUtils.generateJwtToken(
                saasUser.getId(),
                saasUser.getEmail(),
                RoleType.SAAS_USER.name(),
                null,
                "SAAS"
        );

        secondSaasUser = User.builder()
                .firstName("Other")
                .lastName("User")
                .email("other.user@mely.app")
                .password(passwordEncoder.encode("Password123!"))
                .roleType(RoleType.SAAS_USER)
                .createdAt(LocalDateTime.now())
                .build();
        secondSaasUser = userRepository.save(secondSaasUser);
        secondSaasAuthHeader = "Bearer " + jwtUtils.generateJwtToken(
                secondSaasUser.getId(),
                secondSaasUser.getEmail(),
                RoleType.SAAS_USER.name(),
                null,
                "SAAS"
        );
    }

    @AfterEach
    void tearDown() {
        stockScreeningResultRepository.deleteAll();
        stockScreeningDocumentRepository.deleteAll();
        stockScreeningRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void syncManualRequestReturnsCompletedVerdict() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Sukuk Tech")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Software services for banks")
                        .param("sector", "Technology")
                        .param("interestBearingDebt", "20")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "2")
                        .param("investedAmount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andExpect(jsonPath("$.needsTatheer").value(true))
                .andExpect(jsonPath("$.tatheerAmount").value(20.000000))
                .andExpect(jsonPath("$.ruleResults.length()").value(4));
    }

    @Test
    void pdfUploadCreatesAsyncJobAndCanBePolled() throws Exception {
        MockMultipartFile document = samplePdf("financials.pdf");

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(document)
                        .header("Authorization", authHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "Async Capital")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Digital payments software")
                        .param("interestBearingDebt", "20")
                        .param("cashAndInterestBearingAssets", "15")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "1"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();

        JsonNode submitJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String requestId = submitJson.get("requestId").asText();

        mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        JsonNode completed = waitForCompletion(requestId);
        assertThat(completed.get("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void invalidFileTypeIsRejected() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile(
                "documents",
                "financials.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not a pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(invalid)
                        .header("Authorization", authHeader)
                        .param("companyName", "Bad Upload")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Tech"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF documents are supported in v1."));
    }

    @Test
    void missingRequiredBusinessContextIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("companyName", "No Description")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualValuesOverrideExtractedValues() throws Exception {
        MockMultipartFile document = samplePdf("override.pdf");

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(document)
                        .header("Authorization", authHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "Override Corp")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Compliant software business")
                        .param("interestBearingDebt", "20")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "2"))
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        JsonNode completed = waitForCompletion(requestId);

        assertThat(completed.get("verdict").asText()).isEqualTo("HALAL");
        assertThat(completed.at("/metricsUsed/interestBearingDebt/source").asText()).isEqualTo("manual");
        assertThat(completed.at("/metricsUsed/interestBearingDebt/value").decimalValue()).isEqualByComparingTo("20");
    }

    @Test
    void ratioBoundaryAndFailureCasesBehaveAsExpected() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Boundary Ltd")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Cloud software")
                        .param("interestBearingDebt", "33")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"));

        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Failure Ltd")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Cloud software")
                        .param("interestBearingDebt", "34")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_HALAL"));
    }

    @Test
    void nonHalalActivityFailsEvenIfRatiosPass() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Casino Cloud")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Casino software and gaming")
                        .param("interestBearingDebt", "10")
                        .param("cashAndInterestBearingAssets", "5")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_HALAL"));
    }

    @Test
    void purificationGuidanceIsReturnedWithoutHoldings() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Guidance Inc")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Compliant logistics software")
                        .param("interestBearingDebt", "10")
                        .param("cashAndInterestBearingAssets", "5")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andExpect(jsonPath("$.needsTatheer").value(true))
                .andExpect(jsonPath("$.tatheerAmount").value(nullValue()))
                .andExpect(jsonPath("$.purificationRatio").value(0.020000));
    }

    @Test
    void missingMetricsAfterAsyncAnalysisReturnsReviewRequired() throws Exception {
        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "Review Needed")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Sharia-compliant retail technology"))
                .andExpect(status().isAccepted())
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        JsonNode completed = waitForCompletion(requestId);

        assertThat(completed.get("verdict").asText()).isEqualTo("REVIEW_REQUIRED");
    }

    @Test
    void organizationScopingIsPersistedOnCreatedRecords() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Scoped Corp")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Software")
                        .param("interestBearingDebt", "10")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "0"))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = objectMapper.readTree(result.getResponse().getContentAsString()).get("requestId").asText();
        StockScreeningRequest persisted = stockScreeningRequestRepository.findById(UUID.fromString(requestId)).orElseThrow();
        assertThat(persisted.getOrganizationId()).isEqualTo(organization.getId());
        assertThat(persisted.getCreatedByUserId()).isNotNull();
    }

    @Test
    void saasUserCanSubmitAndReadOwnStockRequest() throws Exception {
        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", saasAuthHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "SaaS Screening")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Portfolio analytics software")
                        .param("interestBearingDebt", "10")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        StockScreeningRequest persisted = stockScreeningRequestRepository.findById(UUID.fromString(requestId)).orElseThrow();
        assertThat(persisted.getOrganizationId()).isNull();
        assertThat(persisted.getCreatedByUserId()).isEqualTo(saasUser.getId());

        mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void bankEmployeeRoleCanUseStocksCheck() throws Exception {
        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", loanOfficerAuthHeader)
                        .param("mode", "SYNC")
                        .param("companyName", "Employee Access Co")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Software services")
                        .param("interestBearingDebt", "10")
                        .param("cashAndInterestBearingAssets", "10")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"));
    }

    @Test
    void saasUserHistoryShowsCompletedSyncRequest() throws Exception {
        String requestId = submitSyncScreening(
                saasAuthHeader,
                "History Sync Co",
                "Portfolio analytics software",
                "10",
                "10",
                "100",
                "100",
                "0"
        );

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId))
                .andExpect(jsonPath("$.content[0].companyName").value("History Sync Co"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].verdict").value("HALAL"))
                .andExpect(jsonPath("$.content[0].requestedMode").value("SYNC"));
    }

    @Test
    void saasUserHistoryShowsProcessingAsyncRequestImmediately() throws Exception {
        MockMultipartFile document = samplePdf("history-processing.pdf");

        mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(document)
                        .header("Authorization", saasAuthHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "History Processing Co")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Digital payments software")
                        .param("interestBearingDebt", "20")
                        .param("cashAndInterestBearingAssets", "15")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "1"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyName").value("History Processing Co"))
                .andExpect(jsonPath("$.content[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$.content[0].verdict").value(nullValue()))
                .andExpect(jsonPath("$.content[0].requestedMode").value("ASYNC"));
    }

    @Test
    void completedAsyncRequestAppearsInHistoryWithVerdict() throws Exception {
        MockMultipartFile document = samplePdf("history-completed.pdf");

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(document)
                        .header("Authorization", saasAuthHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "History Completed Co")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Digital payments software")
                        .param("interestBearingDebt", "20")
                        .param("cashAndInterestBearingAssets", "15")
                        .param("totalAssetsOrMarketCap", "100")
                        .param("totalRevenue", "100")
                        .param("nonCompliantIncome", "1"))
                .andExpect(status().isAccepted())
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        waitForStatus(requestId, saasAuthHeader, StockScreeningStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("status", "COMPLETED")
                        .param("query", "History Completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].verdict").value("HALAL"))
                .andExpect(jsonPath("$.content[0].purificationRatio").value(0.010000));
    }

    @Test
    void failedRequestAppearsInHistory() throws Exception {
        MockMultipartFile document = samplePdf("history-failed.pdf");

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .file(document)
                        .header("Authorization", saasAuthHeader)
                        .param("mode", "ASYNC")
                        .param("companyName", "Broken Extract")
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", "Digital payments software"))
                .andExpect(status().isAccepted())
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        waitForStatus(requestId, saasAuthHeader, StockScreeningStatus.FAILED);

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].verdict").value(nullValue()))
                .andExpect(jsonPath("$.content[0].userMessage").value("Processing failed: Simulated extraction failure"));
    }

    @Test
    void historyReturnsOnlyRecordsCreatedByAuthenticatedUser() throws Exception {
        submitSyncScreening(
                saasAuthHeader,
                "First User Co",
                "Portfolio analytics software",
                "10",
                "10",
                "100",
                "100",
                "0"
        );
        submitSyncScreening(
                secondSaasAuthHeader,
                "Second User Co",
                "Treasury software",
                "10",
                "10",
                "100",
                "100",
                "0"
        );

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("First User Co"));
    }

    @Test
    void historySupportsVerdictFilterSearchPaginationAndNewestFirstSort() throws Exception {
        submitSyncScreening(
                saasAuthHeader,
                "Older Halal Co",
                "Portfolio analytics software",
                "10",
                "10",
                "100",
                "100",
                "0"
        );
        Thread.sleep(10);
        submitSyncScreening(
                saasAuthHeader,
                "Newest Non Halal Co",
                "Casino software and gaming",
                "10",
                "5",
                "100",
                "100",
                "0"
        );

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("verdict", "NON_HALAL")
                        .param("query", "newest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyName").value("Newest Non Halal Co"))
                .andExpect(jsonPath("$.content[0].verdict").value("NON_HALAL"));

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].companyName").value("Newest Non Halal Co"));
    }

    @Test
    void detailEndpointsStillWorkForRequestReturnedByHistory() throws Exception {
        String requestId = submitSyncScreening(
                saasAuthHeader,
                "History Detail Co",
                "Portfolio analytics software",
                "10",
                "10",
                "100",
                "100",
                "0"
        );

        mockMvc.perform(get("/api/v1/stocks/check/history")
                        .header("Authorization", saasAuthHeader)
                        .param("query", "History Detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId));

        mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId));

        mockMvc.perform(get("/api/v1/stocks/check/{requestId}/explanation", requestId)
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    private JsonNode waitForCompletion(String requestId) throws Exception {
        for (int i = 0; i < 20; i++) {
            MvcResult pollResult = mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(pollResult.getResponse().getContentAsString());
            if (StockScreeningStatus.COMPLETED.name().equals(json.get("status").asText())) {
                return json;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Request did not complete in time");
    }

    private void waitForStatus(String requestId, String authorizationHeader, StockScreeningStatus expectedStatus) throws Exception {
        for (int i = 0; i < 20; i++) {
            MvcResult pollResult = mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                            .header("Authorization", authorizationHeader))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(pollResult.getResponse().getContentAsString());
            if (expectedStatus.name().equals(json.get("status").asText())) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Request did not reach expected status " + expectedStatus);
    }

    private String submitSyncScreening(String authorizationHeader,
                                      String companyName,
                                      String businessDescription,
                                      String interestBearingDebt,
                                      String cashAndInterestBearingAssets,
                                      String totalAssetsOrMarketCap,
                                      String totalRevenue,
                                      String nonCompliantIncome) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/stocks/check")
                        .header("Authorization", authorizationHeader)
                        .param("mode", "SYNC")
                        .param("companyName", companyName)
                        .param("countryOrMarket", "Tunisia")
                        .param("currency", "TND")
                        .param("businessDescription", businessDescription)
                        .param("interestBearingDebt", interestBearingDebt)
                        .param("cashAndInterestBearingAssets", cashAndInterestBearingAssets)
                        .param("totalAssetsOrMarketCap", totalAssetsOrMarketCap)
                        .param("totalRevenue", totalRevenue)
                        .param("nonCompliantIncome", nonCompliantIncome))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("requestId").asText();
    }

    private MockMultipartFile samplePdf(String fileName) {
        return new MockMultipartFile(
                "documents",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 sample".getBytes()
        );
    }

    @TestConfiguration
    static class StockTestConfig {
        @Bean
        @Primary
        FinancialMetricsExtractor financialMetricsExtractor() {
            return new FinancialMetricsExtractor() {
                @Override
                public ExtractionResult extract(com.example.mely.stocks.model.StockScreeningRequest request, List<com.example.mely.stocks.model.StockScreeningDocument> documents) {
                    Map<String, ExtractedMetric> metrics = new LinkedHashMap<>();
                    List<String> warnings = new ArrayList<>();

                    if (!documents.isEmpty()) {
                        if ("Broken Extract".equals(request.getCompanyName())) {
                            throw new IllegalStateException("Simulated extraction failure");
                        }
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        metrics.put("interestBearingDebt", ExtractedMetric.builder()
                                .value(new BigDecimal("90"))
                                .sourceDocument(documents.getFirst().getOriginalFileName())
                                .pageNumber(2)
                                .snippet("Interest-bearing debt: 90")
                                .build());
                        metrics.put("cashAndInterestBearingAssets", ExtractedMetric.builder()
                                .value(new BigDecimal("40"))
                                .sourceDocument(documents.getFirst().getOriginalFileName())
                                .pageNumber(3)
                                .snippet("Interest-bearing cash: 40")
                                .build());
                        metrics.put("totalAssetsOrMarketCap", ExtractedMetric.builder()
                                .value(new BigDecimal("100"))
                                .sourceDocument(documents.getFirst().getOriginalFileName())
                                .pageNumber(4)
                                .snippet("Total assets: 100")
                                .build());
                        metrics.put("totalRevenue", ExtractedMetric.builder()
                                .value(new BigDecimal("100"))
                                .sourceDocument(documents.getFirst().getOriginalFileName())
                                .pageNumber(5)
                                .snippet("Revenue: 100")
                                .build());
                        metrics.put("nonCompliantIncome", ExtractedMetric.builder()
                                .value(new BigDecimal("2"))
                                .sourceDocument(documents.getFirst().getOriginalFileName())
                                .pageNumber(6)
                                .snippet("Non compliant income: 2")
                                .build());
                        warnings.add("Test extractor supplied placeholder metrics from uploaded PDFs.");
                    }

                    return ExtractionResult.builder()
                            .metrics(metrics)
                            .warnings(warnings)
                            .build();
                }
            };
        }
    }
}
