import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { registerCaregiver } from "../api";

const CAREGIVER_TYPES = [
  { value: "FAMILY", label: "Family Caregiver" },
  { value: "DOCTOR", label: "Doctor / Specialist" },
  { value: "PROFESSIONAL_CAREGIVER", label: "Professional Caregiver" },
  { value: "OTHER", label: "Caregiver" },
];

export default function RegisterPage({ authenticated }) {
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [caregiverType, setCaregiverType] = useState("FAMILY");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [registeredCaregiver, setRegisteredCaregiver] = useState(null);

  if (authenticated) return <Navigate to="/app" replace />;

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
      <div className="auth-card">
        <div className="brand auth-brand">
          <div className="brand-mark">
            <span />
            <span />
            <span />
          </div>
          <div>
            <strong>
              Cogni<span>-</span>Care
            </strong>
            <small>Caregiver support</small>
          </div>
        </div>

        {registeredCaregiver ? (
          <div className="registration-success">
            <span className="eyebrow">Account registered</span>
            <h1 style={{ fontSize: "24px" }}>Welcome to Cogni-Care</h1>
            <p className="auth-copy">
              Your caregiver account has been created successfully. You can now sign in to access your caregiver workspace.
            </p>

            <div className="public-id-card">
              <span className="eyebrow">Your Caregiver Public ID</span>
              <strong className="public-id-display">{registeredCaregiver.public_id}</strong>
              <small>Share this ID with primary caregivers so they can add you to patient care teams.</small>
            </div>

            <div style={{ marginTop: "24px", display: "flex", flexDirection: "column", gap: "10px" }}>
              <Link
                className="button primary"
                to="/login"
                state={{ prefillEmail: registeredCaregiver.email }}
                style={{ textAlign: "center", textDecoration: "none" }}
              >
                Continue to Sign In
              </Link>
            </div>
          </div>
        ) : (
          <>
            <span className="eyebrow">Caregiver registration</span>
            <h1>Create caregiver account</h1>
            <p className="auth-copy">
              Register as a caregiver to support cognitive wellness, monitor gameplay, and coordinate care.
            </p>

            <form onSubmit={submit} className="auth-form">
              <label>
                Full Name
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="e.g. Dr. Sunita Barua"
                  required
                />
              </label>

              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  required
                />
              </label>

              <label>
                Password (min 8 characters)
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  minLength={8}
                  placeholder="••••••••"
                  required
                />
              </label>

              <label>
                Caregiver Role / Designation
                <select
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
              </label>

              <label>
                Phone (optional)
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                />
              </label>

              {error && (
                <p className="form-error" role="alert">
                  {error}
                </p>
              )}

              <button className="button primary" type="submit" disabled={loading}>
                {loading ? "Creating account…" : "Register caregiver"}
              </button>
            </form>

            <div style={{ marginTop: "20px", textAlign: "center" }}>
              <span style={{ fontSize: "12px", color: "#879792" }}>
                Already registered?{" "}
                <Link to="/login" style={{ color: "#185a4e", fontWeight: 700 }}>
                  Sign in
                </Link>
              </span>
            </div>

            <Link className="home-link" to="/">
              ← Back to Home
            </Link>
            <small className="auth-note">
              Web registration is for caregivers and specialists. Patient gameplay is provided on the patient app.
            </small>
          </>
        )}
      </div>
    </div>
  );
}
