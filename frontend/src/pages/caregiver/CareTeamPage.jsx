import { Link, useOutletContext } from "react-router-dom";
import {
  EmptyPatientsState,
  ErrorState,
  LoadingState,
  PageIntro,
  initials,
} from "../../components/caregiver/CaregiverWidgets";
import { Icon } from "../../components/Icons";

export default function CareTeamPage() {
  const data = useOutletContext();

  if (data.loading) return <LoadingState message="Loading care team…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients) return <EmptyPatientsState />;

  const members = data.careTeam || [];
  const patient = data.dashboard?.patient;

  return (
    <>
      <PageIntro
        eyebrow="Patient support"
        title="Care team"
        patient={patient}
        action={
          <Link className="button secondary back-button" to={`/app/patients/${data.selectedId}`}>
            Back to overview
          </Link>
        }
      />
      <section className="panel care-team-panel">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Care team members</span>
            <h2>Authorized caregivers</h2>
          </div>
          <Icon name="users" size={19} />
        </div>
        {members.length ? (
          <div className="care-team-list" style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {members.map((member) => (
              <div
                key={member.caregiver_id}
                className="patient-list-card"
                style={{ justifyContent: "space-between", padding: "16px 20px" }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                  <div className={`avatar ${member.is_primary ? "doctor" : "patient"}`}>
                    {initials(member.display_name)}
                  </div>
                  <div>
                    <h2 style={{ fontSize: "15px", margin: "0 0 3px" }}>{member.display_name}</h2>
                    <p style={{ margin: 0, fontSize: "12px", color: "#879792" }}>
                      {member.caregiver_type === "FAMILY" ? "Family Caregiver" : "Professional Caregiver"}
                    </p>
                  </div>
                </div>
                <div>
                  {member.is_primary ? (
                    <span
                      className="status-pill"
                      style={{
                        background: "#e1f3e9",
                        color: "#237255",
                        fontWeight: 700,
                        padding: "5px 11px",
                        fontSize: "11px",
                      }}
                    >
                      Primary Caregiver
                    </span>
                  ) : (
                    <span
                      className="status-pill"
                      style={{
                        background: "#f0f4f2",
                        color: "#6e837e",
                        fontWeight: 600,
                        padding: "5px 11px",
                        fontSize: "11px",
                      }}
                    >
                      Secondary Caregiver
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No care team members found or access restricted.</p>
        )}
      </section>
    </>
  );
}
