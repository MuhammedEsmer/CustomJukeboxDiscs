import assert from "node:assert/strict";
import { once } from "node:events";
import test from "node:test";
import { createImportServer, ServiceError } from "../src/server.js";

async function withServer(provider, action) {
  const server = createImportServer({ token: "secret", provider, maxConcurrent: 1 });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  try {
    const { port } = server.address();
    await action(`http://127.0.0.1:${port}/v1/import`);
  } finally {
    server.close();
    await once(server, "close");
  }
}

function request(url, body, token = "secret") {
  return fetch(url, {
    method: "POST",
    headers: {
      authorization: `Bearer ${token}`,
      "content-type": "application/json"
    },
    body: JSON.stringify(body)
  });
}

const validRequest = {
  videoId: "xAWDqdpOlu8",
  maxBytes: 26_214_400,
  maxDurationSeconds: 600
};

test("rejects requests with the wrong service token", async () => {
  await withServer(async () => Buffer.from("unused"), async (url) => {
    const response = await request(url, validRequest, "wrong");
    assert.equal(response.status, 401);
    assert.deepEqual(await response.json(), { error: "unauthorized" });
  });
});

test("rejects malformed YouTube video ids before calling the provider", async () => {
  let calls = 0;
  await withServer(async () => {
    calls += 1;
    return Buffer.from("unused");
  }, async (url) => {
    const response = await request(url, { ...validRequest, videoId: "bad" });
    assert.equal(response.status, 400);
    assert.equal(calls, 0);
  });
});

test("returns provider MP3 bytes and resolved title", async () => {
  await withServer(async (request) => {
    assert.deepEqual(request, validRequest);
    return { audio: Buffer.from("fake mp3"), title: "Billie Eilish - WILDFLOWER" };
  }, async (url) => {
    const response = await request(url, validRequest);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get("content-type"), "audio/mpeg");
    assert.equal(response.headers.get("x-cjd-track-title-b64"), "QmlsbGllIEVpbGlzaCAtIFdJTERGTE9XRVI");
    assert.equal(await response.text(), "fake mp3");
  });
});

test("maps provider duration rejection to a stable response", async () => {
  await withServer(async () => {
    throw new ServiceError("too_long", 422);
  }, async (url) => {
    const response = await request(url, validRequest);
    assert.equal(response.status, 422);
    assert.deepEqual(await response.json(), { error: "too_long" });
  });
});
