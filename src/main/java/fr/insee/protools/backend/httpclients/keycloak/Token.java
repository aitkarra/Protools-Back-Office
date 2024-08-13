package fr.insee.protools.backend.httpclients.keycloak;

class Token {
    public final String value;
    public final long endValidityTimeMillis;

    public Token(String value, long endValidityTimeMillis) {
        this.value = value;
        this.endValidityTimeMillis = endValidityTimeMillis;
    }
}
