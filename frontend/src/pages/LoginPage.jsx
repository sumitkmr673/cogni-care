import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";

const demoEmail = import.meta.env.VITE_DEMO_CAREGIVER_EMAIL || "demo.caregiver@cogni-care.example";
const demoPassword = import.meta.env.VITE_DEMO_CAREGIVER_PASSWORD || "DemoCaregiverOnly-2026!";

export default function LoginPage({ authenticated, onSignIn }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState(demoEmail);
  const [password, setPassword] = useState(demoPassword);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (authenticated) return <Navigate to="/app" replace />;

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await onSignIn(email.trim(), password);
      navigate(location.state?.from?.pathname || "/app", { replace: true });
    } catch (requestError) {
      setError(requestError.status === 401 ? "Email or password is incorrect." : "Unable to sign in right now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return <div className="auth-shell"><div className="auth-card"><div className="brand auth-brand"><div className="brand-mark"><span /><span /><span /></div><div><strong>Cogni<span>-</span>Care</strong><small>Caregiver support</small></div></div><span className="eyebrow">Caregiver portal</span><h1>Welcome back</h1><p className="auth-copy">Sign in to view assigned patient activity and cognitive game performance.</p><form onSubmit={submit} className="auth-form"><label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button primary" type="submit" disabled={loading}>{loading ? "Signing in…" : "Sign in"}</button></form><section className="demo-access"><span className="eyebrow">Demo access</span><p>Use the seeded development caregiver account for the SIH demonstration.</p><code>{demoEmail}</code><code>{demoPassword}</code><button type="button" className="text-button" onClick={() => { setEmail(demoEmail); setPassword(demoPassword); }}>Use demo account</button></section><Link className="home-link" to="/">← Back to Home</Link><small className="auth-note">Development prototype · access is provided by the FastAPI backend.</small></div></div>;
}
