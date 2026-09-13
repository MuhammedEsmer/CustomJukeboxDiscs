import { createHash } from "node:crypto";
import { ServiceError } from "./server.js";

export function createRapidApiProvider({ apiKey, username, fetchImpl = fetch, sleep = delay }) {
  if (!apiKey || !username) throw new Error("RapidAPI credentials are required");
  const runHeader = createHash("md5").update(username).digest("hex");

  return async ({ videoId, maxBytes, maxDurationSeconds }) => {
    const metadata = await getJson(
      `https://yt-api.p.rapidapi.com/video/info?id=${encodeURIComponent(videoId)}`,
      "yt-api.p.rapidapi.com",
      apiKey,
      fetchImpl
    );
    const duration = Number(metadata.lengthSeconds);
    if (!Number.isFinite(duration) || duration < 1) throw new ServiceError("unavailable", 422);
    if (duration > maxDurationSeconds) throw new ServiceError("too_long", 422);

    let link;
    for (let attempt = 1; attempt <= 6; attempt += 1) {
      const result = await getJson(
        `https://youtube-mp36.p.rapidapi.com/dl?id=${encodeURIComponent(videoId)}`,
        "youtube-mp36.p.rapidapi.com",
        apiKey,
        fetchImpl
      );
      if (result.status === "ok" && result.link) {
        link = result.link;
        break;
      }
      if (result.status !== "processing") throw new ServiceError("unavailable", 422);
      await sleep(1500 * attempt);
    }
    if (!link) throw new ServiceError("provider_failed", 502);

    const response = await fetchImpl(link, { headers: { "x-run": runHeader } });
    if (!response.ok) throw new ServiceError("provider_failed", 502);
    const declared = Number(response.headers.get("content-length"));
    if (Number.isFinite(declared) && declared > maxBytes) throw new ServiceError("too_large", 413);
    return readBounded(response.body, maxBytes);
  };
}

async function getJson(url, host, apiKey, fetchImpl) {
  const response = await fetchImpl(url, {
    headers: { "x-rapidapi-key": apiKey, "x-rapidapi-host": host }
  });
  if (response.status === 429) throw new ServiceError("rate_limited", 429);
  if (!response.ok) throw new ServiceError("provider_failed", 502);
  try {
    return await response.json();
  } catch {
    throw new ServiceError("provider_failed", 502);
  }
}

async function readBounded(body, maxBytes) {
  if (!body) throw new ServiceError("provider_failed", 502);
  const chunks = [];
  let total = 0;
  for await (const chunk of body) {
    const buffer = Buffer.from(chunk);
    total += buffer.length;
    if (total > maxBytes) throw new ServiceError("too_large", 413);
    chunks.push(buffer);
  }
  return Buffer.concat(chunks);
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
