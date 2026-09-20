import { Link } from "react-router-dom";

export function AuthNavbar() {
  return (
    <header className="landing-header">
      <nav className="landing-nav" aria-label="Main Navigation">
        <Link to="/" className="brand landing-brand" aria-label="Cogni-Care Home">
          <div className="brand-mark" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div>
            <strong>
              Cogni<span>-</span>Care
            </strong>
            <small>Cognitive Care Platform</small>
          </div>
        </Link>

        <div className="landing-nav-links">
          <a href="/#how-it-works" className="nav-anchor">How it works</a>
          <a href="/#for-patients" className="nav-anchor">For patients</a>
          <a href="/#for-caregivers" className="nav-anchor">For caregivers</a>
        </div>

        <div className="landing-nav-actions">
          <Link to="/" className="landing-btn landing-btn-secondary nav-cta">
            Back to home
          </Link>
        </div>
      </nav>
    </header>
  );
}
