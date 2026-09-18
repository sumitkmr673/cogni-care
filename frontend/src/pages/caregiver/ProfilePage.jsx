import { useOutletContext } from "react-router-dom";
import { PageIntro } from "../../components/caregiver/CaregiverWidgets";

function formatCaregiverType(type) {
  switch (type) {
    case "FAMILY":
      return "Family Caregiver";
    case "DOCTOR":
      return "Doctor / Specialist";
    case "PROFESSIONAL_CAREGIVER":
      return "Professional Caregiver";
    case "OTHER":
    default:
      return "Caregiver";
  }
}

export default function ProfilePage({ caregiver, onSignOut }) {
  useOutletContext();
  const initials = caregiver?.display_name
    ?.split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase() || "CC";

  return (
    <>
      <PageIntro eyebrow="Account" title="Caregiver profile" />
      <section className="profile-page-card">
        <div className="avatar doctor profile-avatar">{initials}</div>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
            <span className="eyebrow" style={{ margin: 0 }}>
              Authenticated caregiver
            </span>
            {caregiver?.public_id && <span className="public-id-badge">{caregiver.public_id}</span>}
          </div>
          <h2>{caregiver?.display_name || "Caregiver"}</h2>
          <p style={{ margin: "2px 0 6px" }}>
            {formatCaregiverType(caregiver?.caregiver_type)} · {caregiver?.email || "No email"}
            {caregiver?.phone ? ` · ${caregiver.phone}` : ""}
          </p>
          <small style={{ color: "#879792", fontSize: "11px" }}>
            Share your Public ID (<strong>{caregiver?.public_id || "—"}</strong>) with primary caregivers to be added to patient care teams.
          </small>
        </div>
        <button className="button secondary" type="button" onClick={onSignOut} style={{ marginLeft: "auto" }}>
          Sign out
        </button>
      </section>
    </>
  );
}

