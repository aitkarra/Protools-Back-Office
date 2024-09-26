package fr.insee.protools.backend.restclient.keycloak;

record Token(String value, long endValidityTimeMillis) {
}
