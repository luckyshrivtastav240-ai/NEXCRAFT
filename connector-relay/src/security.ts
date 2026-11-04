import { createHash, randomBytes, timingSafeEqual } from "node:crypto";

const secret = process.env.PAIRING_SIGNING_SECRET ?? "";
if (!/^[a-fA-F0-9]{64,}$/.test(secret)) {
  throw new Error("PAIRING_SIGNING_SECRET must contain at least 32 random bytes (hex).");
}
export const hash = (value: string) => createHash("sha256").update(secret).update(value).digest("hex");
export const randomToken = (bytes = 32) => randomBytes(bytes).toString("base64url");
export const newCode = () => randomBytes(5).toString("hex").toUpperCase();
export function safeEqual(a: string, b: string) {
  const left = Buffer.from(a), right = Buffer.from(b);
  return left.length === right.length && timingSafeEqual(left, right);
}
export function bearer(header: string | undefined) {
  const match = header?.match(/^Bearer ([A-Za-z0-9._~-]{20,200})$/);
  return match?.[1] ?? null;
}
