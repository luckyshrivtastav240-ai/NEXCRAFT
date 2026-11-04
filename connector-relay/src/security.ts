import { createHash, randomBytes, timingSafeEqual, createCipheriv, createDecipheriv } from "node:crypto";

const secret = process.env.PAIRING_SIGNING_SECRET ?? "";
if (!/^[a-fA-F0-9]{64,}$/.test(secret)) {
  throw new Error("PAIRING_SIGNING_SECRET must contain at least 32 random bytes (hex).");
}
export const hash = (value: string) => createHash("sha256").update(secret).update(value).digest("hex");
const encryptionKey = createHash("sha256").update(secret).digest();
export function seal(value: string) {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", encryptionKey, iv);
  const encrypted = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);
  return [iv.toString("base64url"), cipher.getAuthTag().toString("base64url"), encrypted.toString("base64url")].join(".");
}
export function unseal(value: string) {
  const [iv, tag, encrypted] = value.split(".");
  if (!iv || !tag || !encrypted) throw new Error("Invalid encrypted token");
  const decipher = createDecipheriv("aes-256-gcm", encryptionKey, Buffer.from(iv, "base64url"));
  decipher.setAuthTag(Buffer.from(tag, "base64url"));
  return Buffer.concat([decipher.update(Buffer.from(encrypted, "base64url")), decipher.final()]).toString("utf8");
}
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
