import { useState } from "react";
import { Link, useOutletContext } from "react-router-dom";
import {
  EmptyPatientsState,
  ErrorState,
  LoadingState,
  PageIntro,
  initials,
} from "../../components/caregiver/CaregiverWidgets";
import { Icon } from "../../components/Icons";

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

export default function CareTeamPage() {
  const data = useOutletContext();
  const [showAddModal, setShowAddModal] = useState(false);
  const [caregiverPublicId, setCaregiverPublicId] = useState("");
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState("");

  // Confirmation state: { type: "transfer" | "remove", member: object } | null
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState("");

  if (data.loading) return <LoadingState message="Loading care team…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients) return <EmptyPatientsState />;

  const members = data.careTeam || [];
  const patient = data.dashboard?.patient;
  const isPrimary = Boolean(data.dashboard?.caregiver_relationship?.is_primary);

  async function handleAddCaregiver(event) {
    event.preventDefault();
    if (!caregiverPublicId.trim()) return;
    setModalLoading(true);
    setModalError("");
    try {
      await data.addCareTeamMember(caregiverPublicId.trim().toUpperCase());
      setShowAddModal(false);
      setCaregiverPublicId("");
    } catch (err) {
      setModalError(err.message || "Failed to add caregiver");
    } finally {
      setModalLoading(false);
    }
  }

  async function handleConfirmAction() {
    if (!confirmDialog) return;
    setActionLoading(true);
    setActionError("");
    try {
      if (confirmDialog.type === "transfer") {
        await data.transferPrimary(confirmDialog.member.caregiver_id);
      } else if (confirmDialog.type === "remove") {
        await data.removeCareTeamMember(confirmDialog.member.caregiver_id);
      }
      setConfirmDialog(null);
    } catch (err) {
      setActionError(err.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  }

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
          {isPrimary && (
            <button
              type="button"
              className="button primary"
              onClick={() => {
                setShowAddModal(true);
                setModalError("");
              }}
              style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
            >
              + Add Caregiver
            </button>
          )}
        </div>

        {!isPrimary && (
          <div className="notice-box">
            <Icon name="users" size={17} />
            <p>
              You are connected as a secondary caregiver. Only the primary caregiver can add caregivers, transfer primary responsibility, or manage care team access.
            </p>
          </div>
        )}

        {actionError && (
          <p className="form-error" style={{ marginBottom: "14px" }} role="alert">
            {actionError}
          </p>
        )}

        {members.length ? (
          <div className="care-team-list" style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {members.map((member) => (
              <div key={member.caregiver_id} className="care-team-card">
                <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                  <div className={`avatar ${member.is_primary ? "doctor" : "patient"}`}>
                    {initials(member.display_name)}
                  </div>
                  <div>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "3px" }}>
                      <h2 style={{ fontSize: "15px", margin: 0 }}>{member.display_name}</h2>
                      {member.public_id && (
                        <span className="public-id-badge">{member.public_id}</span>
                      )}
                    </div>
                    <p style={{ margin: 0, fontSize: "12px", color: "#879792" }}>
                      {formatCaregiverType(member.caregiver_type)}
                    </p>
                  </div>
                </div>

                <div className="care-team-actions">
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
                    <>
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

                      {isPrimary && (
                        <>
                          <button
                            type="button"
                            className="button secondary"
                            style={{ padding: "5px 9px", fontSize: "11px" }}
                            onClick={() => {
                              setActionError("");
                              setConfirmDialog({ type: "transfer", member });
                            }}
                          >
                            Make Primary
                          </button>
                          <button
                            type="button"
                            className="danger-button"
                            style={{ padding: "5px 9px", fontSize: "11px" }}
                            onClick={() => {
                              setActionError("");
                              setConfirmDialog({ type: "remove", member });
                            }}
                          >
                            Remove
                          </button>
                        </>
                      )}
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No care team members found or access restricted.</p>
        )}
      </section>

      {/* Add Caregiver Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)} role="dialog" aria-modal="true">
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">Care Team Management</span>
                <h2>Add Caregiver</h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setShowAddModal(false)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddCaregiver} className="auth-form">
              <div className="info-callout">
                <p>
                  <strong>Secondary Caregiver Assignment:</strong> Enter the caregiver's public ID (CG-XXXXXXXX). Added caregivers receive secondary access to view this patient's game activity and reminders.
                </p>
              </div>

              <label>
                Caregiver Public ID *
                <input
                  type="text"
                  value={caregiverPublicId}
                  onChange={(e) => setCaregiverPublicId(e.target.value.toUpperCase())}
                  placeholder="CG-XXXXXXXX"
                  maxLength={30}
                  required
                />
              </label>

              {modalError && (
                <p className="form-error" role="alert">
                  {modalError}
                </p>
              )}

              <div className="modal-footer">
                <button
                  type="button"
                  className="button secondary"
                  onClick={() => setShowAddModal(false)}
                  disabled={modalLoading}
                >
                  Cancel
                </button>
                <button type="submit" className="button primary" disabled={modalLoading}>
                  {modalLoading ? "Adding…" : "Add Caregiver"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmDialog && (
        <div className="modal-overlay" onClick={() => setConfirmDialog(null)} role="dialog" aria-modal="true">
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">Confirmation Required</span>
                <h2>
                  {confirmDialog.type === "transfer"
                    ? "Transfer Primary Responsibility"
                    : "Remove Caregiver"}
                </h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setConfirmDialog(null)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <p style={{ fontSize: "13px", color: "#556b65", lineHeight: 1.6, margin: "0 0 16px" }}>
              {confirmDialog.type === "transfer" ? (
                <>
                  Are you sure you want to transfer primary caregiver responsibility for{" "}
                  <strong>{patient?.display_name}</strong> to{" "}
                  <strong>{confirmDialog.member.display_name}</strong> ({confirmDialog.member.public_id})?
                  <br />
                  <br />
                  You will become a secondary caregiver and will no longer have authority to manage caregiver access.
                </>
              ) : (
                <>
                  Are you sure you want to remove{" "}
                  <strong>{confirmDialog.member.display_name}</strong> ({confirmDialog.member.public_id}) from the care team for{" "}
                  <strong>{patient?.display_name}</strong>?
                  <br />
                  <br />
                  They will lose access to this patient&apos;s workspace.
                </>
              )}
            </p>

            {actionError && (
              <p className="form-error" role="alert" style={{ marginBottom: "12px" }}>
                {actionError}
              </p>
            )}

            <div className="modal-footer">
              <button
                type="button"
                className="button secondary"
                onClick={() => setConfirmDialog(null)}
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="button"
                className={confirmDialog.type === "transfer" ? "button primary" : "danger-button"}
                onClick={handleConfirmAction}
                disabled={actionLoading}
              >
                {actionLoading
                  ? "Processing…"
                  : confirmDialog.type === "transfer"
                  ? "Confirm Transfer"
                  : "Confirm Removal"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

