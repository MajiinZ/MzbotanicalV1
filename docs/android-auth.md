# Android sign-in and password recovery

Android keeps the Supabase session between launches. It stores session tokens with an Android Keystore key in app-private, non-backed-up storage, not the account password. The existing age check still applies. Signing out erases the saved session, even if the server is unreachable.

## Recovery email in Supabase

The app accepts the default reset link: copy it from the email without opening it, then paste it into the app. It also accepts numeric reset codes. If custom SMTP is configured, the [project's Reset password email template](https://supabase.com/dashboard/project/dwdyywvzhzjmtfzhyywm/auth/templates/reset-password) can send a code instead. See [Supabase email-template variables](https://supabase.com/docs/guides/auth/auth-email-templates).

```html
<h2>Reset your MZBotanicals password</h2>
<p>Enter this one-time code in the Android app:</p>
<p><strong>{{ .Token }}</strong></p>
<p>If you didn't request a password reset, you can ignore this email.</p>
```

If delivery is limited to project team email addresses, configure [custom SMTP](https://supabase.com/docs/guides/auth/auth-smtp) before offering recovery to customers.

## Test on a phone

1. Sign in with a test account; force-stop and reopen the app. After the age check it should open the store without asking for the password.
2. Sign out, force-stop, and reopen. After the age check it should stay on the sign-in screen. Repeat with the device offline after signing in to verify local sign-out.
3. Tap **Forgot password?** and send a reset email to your test account. Enter an invalid code first, then paste the link from the email (without opening it) or enter its code and set a new password. The old password should fail and the new password should sign you in. The reset itself should not sign you in.
