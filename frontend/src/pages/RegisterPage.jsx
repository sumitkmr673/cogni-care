import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { registerCaregiver } from "../api";
import { AuthNavbar } from "../components/AuthNavbar";
import { ConnectedCareVisual } from "../components/HumanIllustrations";
import { Icon } from "../components/Icons";

const CAREGIVER_TYPES = [
  { value: "FAMILY", label: "Family Caregiver" },
  { value: "DOCTOR", label: "Doctor / Specialist" },
  { value: "PROFESSIONAL_CAREGIVER", label: "Professional Caregiver" },
  { value: "OTHER", label: "Caregiver" },
];

export default function RegisterPage({ authenticated }) {
  const [step, setStep] = useState(1);
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [caregiverType, setCaregiverType] = useState("FAMILY");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [registeredCaregiver, setRegisteredCaregiver] = useState(null);

  if (authenticated) return <Navigate to="/app" replace />;

  function handleContinue(event) {
    event.preventDefault();
    setError("");
    if (!displayName.trim()) {
      setError("Please enter your full name.");
      return;
    }
    if (!email.trim() || !email.includes("@")) {
      setError("Please enter a valid email address.");
      return;
    }
    if (!password || password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    setStep(2);
  }

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const payload = {
        display_name: displayName.trim(),
        email: email.trim().toLowerCase(),
        password,
        caregiver_type: caregiverType,
        phone: phone.trim() ? phone.trim() : null,
      };
      const result = await registerCaregiver(payload);
      setRegisteredCaregiver(result);
    } catch (requestError) {
      setError(requestError.message || "Registration failed. Please try again.");
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
          <p className="auth-left-caption">Care, made simpler.</p>
        </section>

        {/* Right: Focused Caregiver Registration Card */}
        <section className="auth-card-column" aria-label="Caregiver Registration">
          <div className="auth-card auth-register-card">
            {registeredCaregiver ? (
              <div className="registration-success">
                <span className="auth-portal-badge">ACCOUNT REGISTERED</span>
                <h2 className="auth-title">Welcome to Cogni-Care</h2>
                <p className="auth-step-desc">
                  Your caregiver account has been created successfully. You can now sign in to access your caregiver workspace.
                </p>

                <div className="public-id-card">
                  <span className="eyebrow">Your Caregiver Public ID</span>
                  <strong className="public-id-display">{registeredCaregiver.public_id}</strong>
                  <small>Share this ID with primary caregivers so they can add you to patient care teams.</small>
                </div>

                <Link
                  className="button primary auth-submit-btn"
                  to="/login"
                  state={{ prefillEmail: registeredCaregiver.email }}
                  style={{ marginTop: "20px" }}
                >
                  Continue to Sign In →
                </Link>
              </div>
            ) : (
              <>
                <span className="auth-portal-badge">CAREGIVER REGISTRATION</span>
                <h2 className="auth-title">Create your account</h2>

                {/* Compact Step Indicator */}
                <div className="auth-step-indicator" aria-label="Registration progress">
                  <div className="step-track">
                    <div className={`step-node ${step >= 1 ? "active" : ""}`}>
                      <span className="step-num">1</span>
                    </div>
                    <div className={`step-line ${step >= 2 ? "active" : ""}`} />
                    <div className={`step-node ${step >= 2 ? "active" : ""}`}>
                      <span className="step-num">2</span>
                    </div>
                  </div>
                  <div className="step-labels">
                    <span className={`step-label ${step === 1 ? "current" : ""}`}>Your account</span>
                    <span className={`step-label ${step === 2 ? "current" : ""}`}>Your details</span>
                  </div>
                </div>

                <p className="auth-step-desc">
                  {step === 1 ? "Let's start with a few basic details." : "Add your role and contact information."}
                </p>

                {step === 1 ? (
                  <form onSubmit={handleContinue} className="auth-form">
                    <div className="auth-field">
                      <label htmlFor="reg-name">Full name</label>
                      <div className="auth-input-wrap">
                        <span className="auth-input-icon" aria-hidden="true">
                          <Icon name="user" size={17} />
                        </span>
                        <input
                          id="reg-name"
                          type="text"
                          value={displayName}
                          onChange={(e) => setDisplayName(e.target.value)}
                          placeholder="e.g. Dr. Sunita Barua"
                          autoComplete="name"
                          required
                        />
                      </div>
                    </div>

                    <div className="auth-fields-row">
                      <div className="auth-field">
                        <label htmlFor="reg-email">Email</label>
                        <div className="auth-input-wrap">
                          <span className="auth-input-icon" aria-hidden="true">
                            <Icon name="mail" size={17} />
                          </span>
                          <input
                            id="reg-email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="name@example.com"
                            autoComplete="email"
                            required
                          />
                        </div>
                      </div>

                      <div className="auth-field">
                        <label htmlFor="reg-password">Password</label>
                        <div className="auth-input-wrap">
                          <span className="auth-input-icon" aria-hidden="true">
                            <Icon name="lock" size={17} />
                          </span>
                          <input
                            id="reg-password"
                            type={showPassword ? "text" : "password"}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            minLength={8}
                            placeholder="Min. 8 characters"
                            autoComplete="new-password"
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
                    </div>

                    {error && (
                      <div className="form-error-banner" role="alert">
                        <span className="error-icon" aria-hidden="true">!</span>
                        <span>{error}</span>
                      </div>
                    )}

                    <button className="button primary auth-submit-btn" type="submit">
                      Continue →
                    </button>
                  </form>
                ) : (
                  <form onSubmit={submit} className="auth-form">
                    <div className="auth-field">
                      <label htmlFor="reg-role">Caregiver Role / Designation</label>
                      <div className="auth-select-wrap">
                        <select
                          id="reg-role"
                          value={caregiverType}
                          onChange={(e) => setCaregiverType(e.target.value)}
                          className="auth-select"
                        >
                          {CAREGIVER_TYPES.map((type) => (
                            <option key={type.value} value={type.value}>
                              {type.label}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>

                    <div className="auth-field">
                      <label htmlFor="reg-phone">Phone (optional)</label>
                      <div className="auth-input-wrap">
                        <span className="auth-input-icon" aria-hidden="true">
                          <Icon name="phone" size={17} />
                        </span>
                        <input
                          id="reg-phone"
                          type="tel"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          placeholder="+91 98765 43210"
                          autoComplete="tel"
                        />
                      </div>
                    </div>

                    {error && (
                      <div className="form-error-banner" role="alert">
                        <span className="error-icon" aria-hidden="true">!</span>
                        <span>{error}</span>
                      </div>
                    )}

                    <button className="button primary auth-submit-btn" type="submit" disabled={loading}>
                      {loading ? "Creating account…" : "Create caregiver account →"}
                    </button>

                    <button
                      type="button"
                      className="auth-back-step-btn"
                      onClick={() => {
                        setError("");
                        setStep(1);
                      }}
                    >
                      ← Back to account details
                    </button>
                  </form>
                )}

                <div className="auth-card-footer">
                  <span className="auth-footer-prompt">Already have an account?</span>
                  <Link to="/login" className="auth-create-account-btn">
                    Sign in
                  </Link>
                </div>
              </>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
