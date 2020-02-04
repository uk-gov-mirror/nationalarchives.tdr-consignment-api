package http

import org.keycloak.adapters.{HttpClientBuilder, KeycloakDeployment}
import org.keycloak.adapters.rotation.JWKPublicKeyLocator
import org.keycloak.representations.adapters.config.AdapterConfig

class TdrKeycloakDeployment extends KeycloakDeployment {
}

object TdrKeycloakDeployment {
  def apply(authServer: String, realm: String, ttl: Int): TdrKeycloakDeployment = {
    val keycloakDeployment = new TdrKeycloakDeployment()
    keycloakDeployment.setClient(new HttpClientBuilder().build())

    val ttl = 60 * 10
    keycloakDeployment.setPublicKeyCacheTtl(ttl)

    val adaptorConfig = new AdapterConfig()
    adaptorConfig.setAuthServerUrl(authServer)
    keycloakDeployment.setAuthServerBaseUrl(adaptorConfig)

    keycloakDeployment.setRealm(realm)

    keycloakDeployment.setPublicKeyLocator(new JWKPublicKeyLocator)
    keycloakDeployment
  }
}
