import assert from "node:assert/strict";
import test from "node:test";
import { createRapidApiProvider } from "../src/provider.js";

function response(status, body, headers = {}) {
  return new Response(body, { status, headers });
}

test("resolves metadata, polls conversion, and downloads bounded MP3", async () => {
  const calls = [];
  const replies = [
    response(200, JSON.stringify({ lengthSeconds: "288", title: "Billie Eilish - WILDFLOWER" }), { "content-type": "application/json" }),
    response(200, JSON.stringify({ status: "processing" }), { "content-type": "application/json" }),
    response(200, JSON.stringify({ status: "ok", link: "https://audio.example/track.mp3" }), { "content-type": "application/json" }),
    response(200, "fake mp3", { "content-length": "8" })
  ];
  const provider = createRapidApiProvider({
    apiKey: "api-key",
    username: "user",
    fetchImpl: async (url, options) => {
      calls.push({ url: String(url), options });
      return replies.shift();
    },
    sleep: async () => {}
  });

  const result = await provider({ videoId: "xAWDqdpOlu8", maxBytes: 1024, maxDurationSeconds: 600 });

  assert.equal(result.audio.toString(), "fake mp3");
  assert.equal(result.title, "Billie Eilish - WILDFLOWER");
  assert.equal(calls.length, 4);
  assert.match(calls[0].url, /video\/info\?id=xAWDqdpOlu8$/);
  assert.match(calls[1].url, /youtube-mp36/);
  assert.equal(typeof calls[3].options.headers["x-run"], "string");
});

test("rejects tracks over the requested duration before requesting audio", async () => {
  let calls = 0;
  const provider = createRapidApiProvider({
    apiKey: "api-key",
    username: "user",
    fetchImpl: async () => {
      calls += 1;
      return response(200, JSON.stringify({ lengthSeconds: "601" }), { "content-type": "application/json" });
    }
  });

  await assert.rejects(
    provider({ videoId: "xAWDqdpOlu8", maxBytes: 1024, maxDurationSeconds: 600 }),
    (error) => error.code === "too_long" && error.status === 422
  );
  assert.equal(calls, 1);
});
