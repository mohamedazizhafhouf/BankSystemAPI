package com.example.mely.auth;

import com.example.mely.model.Enums.RoleType;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.OrganizationRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.stocks.repository.StockScreeningDocumentRepository;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;
import com.example.mely.stocks.repository.StockScreeningResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthAndEmployeeFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private StockScreeningResultRepository stockScreeningResultRepository;

    @Autowired
    private StockScreeningDocumentRepository stockScreeningDocumentRepository;

    @Autowired
    private StockScreeningRequestRepository stockScreeningRequestRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
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
    void saasUserCanRegisterAndLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Saas",
                                  "lastName": "User",
                                  "email": "saas.user@mely.app",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("saas.user@mely.app"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "saas.user@mely.app",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SAAS_USER"))
                .andExpect(jsonPath("$.accountType").value("SAAS"))
                .andExpect(jsonPath("$.organizationId").value(nullValue()));
    }

    @Test
    void duplicateEmailRegistrationIsRejected() throws Exception {
        registerSaasUser("duplicate@mely.app");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Second",
                                  "lastName": "User",
                                  "email": "duplicate@mely.app",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void provisionedBankAdminCanLoginCreateEmployeesAndEmployeesCanLogin() throws Exception {
        provisionBank("admin@zitouna.tn", "Password123!");
        String adminToken = loginAndGetToken("admin@zitouna.tn", "Password123!");

        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Loan",
                                  "lastName": "Officer",
                                  "email": "loan.officer@zitouna.tn",
                                  "password": "Password123!",
                                  "role": "LOAN_OFFICER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LOAN_OFFICER"));

        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sharia",
                                  "lastName": "Auditor",
                                  "email": "sharia.auditor@zitouna.tn",
                                  "password": "Password123!",
                                  "role": "SHARIA_AUDITOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SHARIA_AUDITOR"));

        mockMvc.perform(get("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "loan.officer@zitouna.tn",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LOAN_OFFICER"))
                .andExpect(jsonPath("$.accountType").value("BANK"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sharia.auditor@zitouna.tn",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SHARIA_AUDITOR"))
                .andExpect(jsonPath("$.accountType").value("BANK"));
    }

    @Test
    void nonAdminBankEmployeeCannotCreateEmployees() throws Exception {
        provisionBank("admin@baraka.tn", "Password123!");
        String adminToken = loginAndGetToken("admin@baraka.tn", "Password123!");

        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Loan",
                                  "lastName": "Officer",
                                  "email": "loan@baraka.tn",
                                  "password": "Password123!",
                                  "role": "LOAN_OFFICER"
                                }
                                """))
                .andExpect(status().isCreated());

        String employeeToken = loginAndGetToken("loan@baraka.tn", "Password123!");
        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Blocked",
                                  "lastName": "User",
                                  "email": "blocked@baraka.tn",
                                  "password": "Password123!",
                                  "role": "SHARIA_AUDITOR"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void saasUserCannotCallBankEmployeeEndpoints() throws Exception {
        registerSaasUser("saas.bank.block@mely.app");
        String saasToken = loginAndGetToken("saas.bank.block@mely.app", "Password123!");

        mockMvc.perform(get("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + saasToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void bankEmployeeCannotCreateAnotherAdminAndSaasUserCanUseStocks() throws Exception {
        provisionBank("admin@nour.tn", "Password123!");
        String adminToken = loginAndGetToken("admin@nour.tn", "Password123!");

        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Auditor",
                                  "lastName": "User",
                                  "email": "auditor@nour.tn",
                                  "password": "Password123!",
                                  "role": "SHARIA_AUDITOR"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bank/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Blocked",
                                  "lastName": "Admin",
                                  "email": "blocked.admin@nour.tn",
                                  "password": "Password123!",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee creation cannot assign the ADMIN role in v1."));

        registerSaasUser("stocks@mely.app");
        String saasToken = loginAndGetToken("stocks@mely.app", "Password123!");

        mockMvc.perform(post("/api/v1/stocks/check")
                        .header("Authorization", "Bearer " + saasToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "SYNC",
                                  "companyName": "SaaS Dashboard Co",
                                  "countryOrMarket": "Tunisia",
                                  "currency": "TND",
                                  "businessDescription": "Portfolio software",
                                  "interestBearingDebt": 10,
                                  "cashAndInterestBearingAssets": 10,
                                  "totalAssetsOrMarketCap": 100,
                                  "totalRevenue": 100,
                                  "nonCompliantIncome": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HALAL"));
    }

    private void registerSaasUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Saas",
                                  "lastName": "User",
                                  "email": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private void provisionBank(String adminEmail, String tempPassword) throws Exception {
        mockMvc.perform(post("/api/v1/system/provision")
                        .header("X-Mely-System-Key", "test-system-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Mohamed",
                                  "lastName": "Admin",
                                  "bankName": "Zitouna Bank",
                                  "adminEmail": "%s",
                                  "tempPassword": "%s"
                                }
                                """.formatted(adminEmail, tempPassword)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
