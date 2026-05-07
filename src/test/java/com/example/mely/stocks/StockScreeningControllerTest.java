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
import com.example.mely.stocks.repository.StockScreeningDocumentRepository;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;
import com.example.mely.stocks.repository.StockScreeningResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private String secondSaaSAuthHeader;
    private String loanOfficerAuthHeader;
    private User saasUser;
    private User secondSaaSUser;

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

        secondSaaSUser = User.builder()
                .firstName("Other")
                .lastName("User")
                .email("other.user@mely.app")
                .password(passwordEncoder.encode("Password123!"))
                .roleType(RoleType.SAAS_USER)
                .createdAt(LocalDateTime.now())
                .build();
        secondSaaSUser = userRepository.save(secondSaaSUser);
        secondSaaSAuthHeader = "Bearer " + jwtUtils.generateJwtToken(
                secondSaaSUser.getId(),
                secondSaaSUser.getEmail(),
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
        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Sukuk Tech",
                                "Software services for banks",
                                "Technology",
                                "20", "10", "100", "100", "2", "1000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andExpect(jsonPath("$.needsTatheer").value(true))
                .andExpect(jsonPath("$.tatheerAmount").value(20.000000))
                .andExpect(jsonPath("$.ruleResults.length()").value(4));
    }

    @Test
    void missingCoreMetricsIsRejected() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("companyName", "No Description");
        body.put("countryOrMarket", "Tunisia");
        body.put("currency", "TND");

        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Manual screening requires all core financial metrics."));
    }

    @Test
    void ratioBoundaryAndFailureCasesBehaveAsExpected() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Boundary Ltd",
                                "Cloud software",
                                "Technology",
                                "33", "10", "100", "100", "1", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"));

        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Failure Ltd",
                                "Cloud software",
                                "Technology",
                                "34", "10", "100", "100", "1", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_HALAL"));
    }

    @Test
    void nonHalalActivityFailsEvenIfRatiosPass() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Casino Cloud",
                                "Casino software and gaming",
                                "Technology",
                                "10", "5", "100", "100", "0", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_HALAL"));
    }

    @Test
    void purificationGuidanceIsReturnedWithoutHoldings() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Guidance Inc",
                                "Compliant logistics software",
                                "Technology",
                                "10", "5", "100", "100", "2", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andExpect(jsonPath("$.needsTatheer").value(true))
                .andExpect(jsonPath("$.tatheerAmount").value(nullValue()))
                .andExpect(jsonPath("$.purificationRatio").value(0.020000));
    }

    @Test
    void organizationScopingIsPersistedOnCreatedRecords() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Scoped Corp",
                                "Software",
                                "Technology",
                                "10", "10", "100", "100", "0", null))))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = objectMapper.readTree(result.getResponse().getContentAsString()).get("requestId").asText();
        StockScreeningRequest persisted = stockScreeningRequestRepository.findById(UUID.fromString(requestId)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getOrganizationId()).isEqualTo(organization.getId());
        org.assertj.core.api.Assertions.assertThat(persisted.getCreatedByUserId()).isNotNull();
    }

    @Test
    void saasUserCanSubmitAndReadOwnStockRequest() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", saasAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "SaaS Screening",
                                "Portfolio analytics software",
                                "Technology",
                                "10", "10", "100", "100", "0", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"))
                .andReturn();

        String requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("requestId").asText();
        StockScreeningRequest persisted = stockScreeningRequestRepository.findById(UUID.fromString(requestId)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getOrganizationId()).isNull();
        org.assertj.core.api.Assertions.assertThat(persisted.getCreatedByUserId()).isEqualTo(saasUser.getId());

        mockMvc.perform(get("/api/v1/stocks/check/{requestId}", requestId)
                        .header("Authorization", saasAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void bankEmployeeRoleCanUseStocksCheck() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", loanOfficerAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                "Employee Access Co",
                                "Software services",
                                "Technology",
                                "10", "10", "100", "100", "0", null))))
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
                secondSaaSAuthHeader,
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

    private Map<String, Object> manualBody(String companyName,
                                          String businessDescription,
                                          String sector,
                                          String interestBearingDebt,
                                          String cashAndInterestBearingAssets,
                                          String totalAssetsOrMarketCap,
                                          String totalRevenue,
                                          String nonCompliantIncome,
                                          String investedAmount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("companyName", companyName);
        body.put("countryOrMarket", "Tunisia");
        body.put("currency", "TND");
        body.put("businessDescription", businessDescription);
        body.put("sector", sector);
        body.put("interestBearingDebt", new java.math.BigDecimal(interestBearingDebt));
        body.put("cashAndInterestBearingAssets", new java.math.BigDecimal(cashAndInterestBearingAssets));
        body.put("totalAssetsOrMarketCap", new java.math.BigDecimal(totalAssetsOrMarketCap));
        body.put("totalRevenue", new java.math.BigDecimal(totalRevenue));
        body.put("nonCompliantIncome", new java.math.BigDecimal(nonCompliantIncome));
        if (investedAmount != null) {
            body.put("investedAmount", new java.math.BigDecimal(investedAmount));
        }
        return body;
    }

    private String submitSyncScreening(String authorizationHeader,
                                      String companyName,
                                      String businessDescription,
                                      String interestBearingDebt,
                                      String cashAndInterestBearingAssets,
                                      String totalAssetsOrMarketCap,
                                      String totalRevenue,
                                      String nonCompliantIncome) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualBody(
                                companyName,
                                businessDescription,
                                "Technology",
                                interestBearingDebt,
                                cashAndInterestBearingAssets,
                                totalAssetsOrMarketCap,
                                totalRevenue,
                                nonCompliantIncome,
                                null))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("requestId").asText();
    }
}
