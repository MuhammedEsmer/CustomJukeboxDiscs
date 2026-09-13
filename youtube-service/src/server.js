import http from "node:http";

const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;

export class ServiceError extends Error {
  constructor(code, status) {
    super(code);
    this.code = code;
    this.status = status;
  }
}

export function createImportServer({ token, provider, maxConcurrent = 2 }) {
  if (!token) throw new Error("CJD_SERVICE_TOKEN is required");
  let active = 0;
  return http.createServer(async (request, response) => {
    try {
      if (request.method !== "POST" || request.url !== "/v1/import") {
        return json(response, 404, "not_found");
      }
      if (request.headers.authorization !== `Bearer ${token}`) {
        return json(response, 401, "unauthorized");
      }
      if (active >= maxConcurrent) return json(response, 429, "busy");
      const input = validate(await readJson(request));
      active += 1;
      try {
        const audio = await provider(input);
        if (!Buffer.isBuffer(audio)) throw new ServiceError("provider_failed", 502);
        if (audio.length > input.maxBytes) throw new ServiceError("too_large", 413);
        response.writeHead(200, {
          "content-type": "audio/mpeg",
          "content-length": audio.length,
          "cache-control": "no-store"
        });
        response.end(audio);
      } finally {
        active -= 1;
      }
    } catch (error) {
      if (error instanceof ServiceError) return json(response, error.status, error.code);
      console.error("YouTube import failed", error);
      return json(response, 502, "provider_failed");
    }
  });
}

function validate(value) {
  if (!value || !VIDEO_ID.test(value.videoId)
      || !Number.isSafeInteger(value.maxBytes) || value.maxBytes < 1
      || !Number.isSafeInteger(value.maxDurationSeconds) || value.maxDurationSeconds < 1) {
    throw new ServiceError("invalid_request", 400);
  }
  return {
    videoId: value.videoId,
    maxBytes: value.maxBytes,
    maxDurationSeconds: value.maxDurationSeconds
  };
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 8192) throw new ServiceError("invalid_request", 400);
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    throw new ServiceError("invalid_request", 400);
  }
}

function json(response, status, error) {
  if (response.headersSent) return response.destroy();
  const body = Buffer.from(JSON.stringify({ error }));
  response.writeHead(status, {
    "content-type": "application/json",
    "content-length": body.length,
    "cache-control": "no-store"
  });
  response.end(body);
}
