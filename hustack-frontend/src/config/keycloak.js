import Keycloak from "keycloak-js";
import getEnv, {config} from "./config";

//
export const initOptions = { pkceMethod: "S256" };
export const KC_REALM = getEnv("REALM");

// Pass initialization options as required or leave blank to load from 'keycloak.json'
const keycloak = new Keycloak({
  url: `${config.url.KEYCLOAK_BASE_URL}`,
  realm: KC_REALM,
  clientId: getEnv('CLIENT_ID'),
});

export default keycloak;
