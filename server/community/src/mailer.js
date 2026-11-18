// Sends the verification-code emails through Cloudflare Email Service (the EMAIL send_email binding in
// wrangler.toml). MAIL_FROM must be an address on a domain onboarded to Email Sending, e.g.
// community@charltonbornstudios.com. Returns a MAILER for app.js, or null when email isn't set up yet.
export function cloudflareMailer(env) {
  if (!env.EMAIL || !env.MAIL_FROM) return null;
  return {
    async send(to, subject, text) {
      await env.EMAIL.send({ to, from: env.MAIL_FROM, subject, text });
    },
  };
}
