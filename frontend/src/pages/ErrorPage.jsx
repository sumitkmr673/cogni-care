import { Link } from "react-router-dom";
import { clearToken } from "../api";

export default function ErrorPage({ onRetry, errorReference }) {
  function handleSignOut() {
    clearToken();
    window.location.href = "/login";
  }

  function handleRetry() {
    if (onRetry) {
      onRetry();
    } else {
      window.location.reload();
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ textAlign: "center" }}>
        <div className="brand auth-brand" style={{ justifyContent: "center" }}>
          <div className="brand-mark">
            <span />
            <span />
            <span />
          </div>
          <div style={{ textAlign: "left" }}>
            <strong>
              Cogni<span>-</span>Care
            </strong>
            <small>Caregiver support</small>
          </div>
        </div>

        <span className="eyebrow" style={{ color: "#b04332" }}>
          System notice
        </span>
        <h1 style={{ fontSize: "24px", margin: "6px 0 10px", color: "#21423d" }}>
          Something went wrong
        </h1>
        <p className="auth-copy" style={{ margin: "0 0 24px" }}>
          An unexpected application error occurred. We apologize for the inconvenience. You can try reloading the workspace or return to sign in.
        </p>

        {errorReference && (
          <div
            style={{
              background: "#fdf8f7",
              border: "1px solid #f3d7d2",
              borderRadius: "8px",
              padding: "8px 12px",
              marginBottom: "20px",
              fontSize: "11px",
              color: "#8c4436",
              textAlign: "left",
              wordBreak: "break-word",
            }}
          >
            <strong>Diagnostic reference:</strong> {errorReference}
          </div>
        )}

        <div style={{ display: "flex", gap: "12px", justifyContent: "center" }}>
          <button type="button" className="button primary" onClick={handleRetry}>
            Try again
          </button>
          <button type="button" className="button secondary" onClick={handleSignOut}>
            Back to Sign In
          </button>
        </div>

        <div style={{ marginTop: "24px" }}>
          <Link to="/" className="home-link" style={{ display: "inline-block", margin: 0 }}>
            ← Home
          </Link>
        </div>
      </div>
    </div>
  );
}
