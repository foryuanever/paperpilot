import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import { initializeApiBaseUrl } from "./services/apiClient";
import "../styles.css";
import "./styles/spatial.css";

async function bootstrap() {
  await initializeApiBaseUrl();

  const app = createApp(App);

  app.use(createPinia());
  app.use(router);

  app.mount("#app");
}

bootstrap();
