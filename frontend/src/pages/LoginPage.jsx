import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { AuthNavbar } from "../components/AuthNavbar";
import { ConnectedCareVisual } from "../components/HumanIllustrations";
import { Icon } from "../components/Icons";

export default function LoginPage({ authenticated, onSignIn }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState(location.state?.prefillEmail || "");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
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

  return (
    <div className="auth-shell">
      {/* Soft Ambient Background Atmosphere */}
      <div className="auth-bg-mint" aria-hidden="true" />
      <div className="auth-bg-peach" aria-hidden="true" />

      {/* Unified Landing Navbar */}
      <AuthNavbar />

      {/* Main Two-Column Layout */}
      <main className="auth-main-layout">
        {/* Left: Prominent Human-Centered Illustration */}
        <section className="auth-visual-column" aria-label="Cognitive care illustration">
          <div className="auth-illustration-wrap" aria-hidden="true">
            <ConnectedCareVisual width="100%" height="auto" />
          </div>
          <p className="auth-left-caption">Support today. Brighter tomorrow.</p>
        </section>

        {/* Right: Focused Caregiver Login Card */}
        <section className="auth-card-column" aria-label="Caregiver Sign In">
          <div className="auth-card auth-login-card">
            <span className="auth-portal-badge">CAREGIVER PORTAL</span>
            <h1 className="auth-title">Welcome back</h1>

            <form onSubmit={submit} className="auth-form">
              <div className="auth-field">
                <label htmlFor="login-email">Email</label>
                <div className="auth-input-wrap">
                  <span className="auth-input-icon" aria-hidden="true">
                    <Icon name="mail" size={17} />
                  </span>
                  <input
                    id="login-email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="name@example.com"
                    autoComplete="email"
                    required
                  />
                </div>
              </div>

              <div className="auth-field">
                <label htmlFor="login-password">Password</label>
                <div className="auth-input-wrap">
                  <span className="auth-input-icon" aria-hidden="true">
                    <Icon name="lock" size={17} />
                  </span>
                  <input
                    id="login-password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Enter your password"
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    className="auth-password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    <Icon name={showPassword ? "eyeOff" : "eye"} size={17} />
                  </button>
                </div>
              </div>

              {error && (
                <div className="form-error-banner" role="alert">
                  <span className="error-icon" aria-hidden="true">!</span>
                  <span>{error}</span>
                </div>
              )}

              <button className="button primary auth-submit-btn" type="submit" disabled={loading}>
                {loading ? "Signing in…" : "Sign in →"}
              </button>
            </form>

            <div className="auth-card-footer">
              <span className="auth-footer-prompt">New to Cogni-Care?</span>
              <Link to="/register" className="auth-create-account-btn">
                Create an account
              </Link>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

