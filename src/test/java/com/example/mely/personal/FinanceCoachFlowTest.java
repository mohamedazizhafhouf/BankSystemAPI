package com.example.mely.personal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mely.personal.repository.FinanceCoachInsightRepository;
import com.example.mely.personal.repository.FinanceCoachMessageRepository;
import com.example.mely.personal.repository.PersonalBudgetRepository;
import com.example.mely.personal.repository.PersonalTransactionRepository;
import com.example.mely.personal.repository.SavingsGoalRepository;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.OrganizationRepository;
import com.example.mely.repository.UserRepository;
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

@SpringBootTest
class FinanceCoachFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FinanceCoachInsightRepository insightRepository;

    @Autowired
    private FinanceCoachMessageRepository messageRepository;

    @Autowired
    private PersonalTransactionRepository transactionRepository;

    @Autowired
    private PersonalBudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository goalRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

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
        messageRepository.deleteAll();
        insightRepository.deleteAll();
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        goalRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void saasUserCanGenerateCachedMonthlyInsightsFromPersonalFinanceContext() throws Exception {
        registerSaasUser("coach@mely.app");
        String token = loginAndGetToken("coach@mely.app", "Password123!");

        mockMvc.perform(post("/api/v1/personal/money/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "occurredOn": "2026-05-04",
                                  "direction": "EXPENSE",
                                  "categoryCode": "FOOD",
                                  "amount": 90,
                                  "memo": "Groceries"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/personal/money/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "yearMonth": "2026-05",
                                  "categoryCode": "FOOD",
                                  "capAmount": 100
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/personal/coach/monthly-insights")
                        .header("Authorization", "Bearer " + token)
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-05"))
                .andExpect(jsonPath("$.healthScore").isNumber())
                .andExpect(jsonPath("$.cards.length()").isNotEmpty());

        mockMvc.perform(get("/api/v1/personal/coach/monthly-insights")
                        .header("Authorization", "Bearer " + token)
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isOk());

        assertThat(insightRepository.findAll()).hasSize(1);
    }

    @Test
    void coachChatPersistsUserAndAssistantMessages() throws Exception {
        registerSaasUser("chat.coach@mely.app");
        String token = loginAndGetToken("chat.coach@mely.app", "Password123!");

        mockMvc.perform(post("/api/v1/personal/coach/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "yearMonth": "2026-05",
                                  "message": "Am I ready for zakat?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.assistantMessage.content").isNotEmpty());

        mockMvc.perform(get("/api/v1/personal/coach/chat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));

        assertThat(messageRepository.findAll()).hasSize(2);
    }

    @Test
    void bankUserCannotAccessPersonalCoach() throws Exception {
        provisionBank("admin@coach-bank.tn", "Password123!");
        String token = loginAndGetToken("admin@coach-bank.tn", "Password123!");

        mockMvc.perform(get("/api/v1/personal/coach/monthly-insights")
                        .header("Authorization", "Bearer " + token)
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isForbidden());
    }

    private void registerSaasUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Coach",
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
                                  "firstName": "Bank",
                                  "lastName": "Admin",
                                  "bankName": "Coach Bank",
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
