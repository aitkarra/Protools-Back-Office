package fr.insee.protools.backend.controller;

import fr.insee.protools.backend.restclient.RestClientHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = StarterController.class)
//@ActiveProfiles(value = "test")
class StarterControllerTest {
    @MockBean
    RestClientHelper restClientHelper;
    @MockBean
    JwtDecoder jwtDecoder;


    @Autowired private MockMvc mockMvc;
    @Test
    void shouldReturnDefaultMessage() throws Exception {
        this.mockMvc.perform(get("/starter/healthcheck").with(jwt()))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("OK")));


                mockMvc.perform(get("/starter/healthcheck").with(jwt()
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_AUTHORIZED_PERSONNEL"))).jwt(jwt -> {
                            jwt.subject("Ch4mpy");
                            jwt.claims(claims -> claims.put(StandardClaimNames.PREFERRED_USERNAME, "Tonton Pirate"));
                        })))
                        .andDo(print())
                        .andExpect(content().string(containsString("OK")))
                        .andExpect(content().string(containsString("Ch4mpy")));

    }

}
