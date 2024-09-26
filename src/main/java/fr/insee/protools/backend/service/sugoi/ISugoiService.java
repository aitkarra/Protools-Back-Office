package fr.insee.protools.backend.service.sugoi;

import fr.insee.protools.backend.dto.sugoi.User;

public interface ISugoiService {

    User postCreateUser(User userBody);

    void postInitPassword(String userId, String password);
}
