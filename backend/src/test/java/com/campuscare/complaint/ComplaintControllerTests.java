package com.campuscare.complaint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.campuscare.auth.StudentAccount;
import com.campuscare.auth.StudentAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import com.campuscare.auth.AuthController;
import com.campuscare.config.CorsConfig;
import com.campuscare.config.SecurityConfig;

@WebMvcTest(controllers = {ComplaintController.class, AuthController.class}, properties = {
    "app.admin.username=admin",
    "app.admin.password=test-password",
    "app.cors.allowed-origins=http://localhost:5175"
})
@Import({CorsConfig.class, SecurityConfig.class})
@SuppressWarnings("null")
class ComplaintControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintRepository complaintRepository;

    @MockBean
    private StudentAccountRepository studentAccountRepository;

    @Test
    void createComplaintReturnsCreatedAndPendingStatus() throws Exception {
        AtomicReference<Complaint> submitted = new AtomicReference<>();
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint complaint = invocation.getArgument(0);
            complaint.assignDefaultStatus();
            submitted.set(complaint);
            return complaint;
        });

        mockMvc.perform(post("/api/complaints").with(user("student@example.com").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"College","location":"Library","title":"Broken light","description":"The light is out"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Complaint submitted successfully"))
                .andExpect(jsonPath("$.status").doesNotExist());
            assertEquals("Pending", submitted.get().getStatus());
    }

    @Test
    void createComplaintRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/complaints").with(user("student@example.com").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getMissingComplaintReturnsNotFoundMessage() throws Exception {
        when(complaintRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/complaints/42").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Complaint 42 was not found"));
    }

    @Test
    void statusCanBeUpdated() throws Exception {
        Complaint complaint = new Complaint("Hostel", "Room 4", "Leaking tap", "Tap is leaking");
        complaint.assignDefaultStatus();
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/complaints/1/status")
                .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"In Progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("In Progress"));
    }

        @Test
        void complaintListRequiresAdminLogin() throws Exception {
        mockMvc.perform(get("/api/complaints"))
            .andExpect(status().isUnauthorized());
        }

        @Test
        void anonymousUsersCannotViewOrChangeComplaintTracking() throws Exception {
            mockMvc.perform(get("/api/complaints/1"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/api/complaints/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"Resolved\"}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/complaints/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void adminLoginCreatesSessionAndGrantsComplaintAccess() throws Exception {
        var login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"test-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/complaints").session(session))
            .andExpect(status().isOk());
        }

        @Test
        void studentRegistrationAcceptsWeakNonEmptyPassword() throws Exception {
        when(studentAccountRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(studentAccountRepository.save(any(StudentAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            String passwordHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("1");
            when(studentAccountRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(new StudentAccount("student@example.com", passwordHash)));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Student@Example.com\",\"password\":\"1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("student@example.com"))
            .andExpect(jsonPath("$.role").value("STUDENT"));
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/student/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@example.com\",\"password\":\"1\"}"))
            .andExpect(status().isOk())
            .andReturn().getRequest().getSession(false);
        assertNotNull(session);
        }

        @Test
        void studentEmailLoginCreatesSessionButCannotReadTracking() throws Exception {
        String passwordHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("x");
        when(studentAccountRepository.findByEmailIgnoreCase("student@example.com"))
            .thenReturn(Optional.of(new StudentAccount("student@example.com", passwordHash)));

        var login = mockMvc.perform(post("/api/auth/student/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Student@Example.com\",\"password\":\"x\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(post("/api/complaints").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"College\",\"location\":\"Library\",\"title\":\"Light out\",\"description\":\"It is dark\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(get("/api/complaints").session(session))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"student@example.com\",\"password\":\"x\"}"))
            .andExpect(status().isUnauthorized());
        }

        @Test
        void anonymousUsersCannotSubmitComplaints() throws Exception {
        mockMvc.perform(post("/api/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"College\",\"location\":\"Library\",\"title\":\"Light out\",\"description\":\"It is dark\"}"))
            .andExpect(status().isUnauthorized());
        }
}
