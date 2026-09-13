import { createRapidApiProvider } from "./provider.js";
import { createImportServer } from "./server.js";

const host = process.env.CJD_SERVICE_HOST || "127.0.0.1";
const port = Number(process.env.CJD_SERVICE_PORT || "8765");
const token = process.env.CJD_SERVICE_TOKEN;
const provider = createRapidApiProvider({
  apiKey: process.env.RAPIDAPI_KEY,
  username: process.env.RAPIDAPI_USERNAME
});
const server = createImportServer({ token, provider, maxConcurrent: 2 });

server.listen(port, host, () => {
  console.log(`Custom Jukebox Discs YouTube service listening on ${host}:${port}`);
});
