const getEnv = (key, defaultValue = "") => {
  return window.ENV?.[key] || defaultValue;
};

export default getEnv;

export const config = {
  url: {
    KEYCLOAK_BASE_URL: getEnv('AUTH_URL'),
    API_URL: getEnv('API_URL'),
  },
};

export const PLATFORM_NAME = getEnv('PLATFORM_NAME', "HUSTack");
