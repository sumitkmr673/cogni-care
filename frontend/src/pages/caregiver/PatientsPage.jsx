import { useEffect, useState } from "react";
import { Link, useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import {
  EmptyPatientsState,
  ErrorState,
  LoadingState,
  PageIntro,
  initials,
} from "../../components/caregiver/CaregiverWidgets";

export default function PatientsPage() {
  const data = useOutletContext();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showModal, setShowModal] = useState(false);
  const [tab, setTab] = useState("create"); // "create" | "link"

  // Create patient form state
  const [displayName, setDisplayName] = useState("");
  const [dob, setDob] = useState("");
  const [gender, setGender] = useState("");
  const [language, setLanguage] = useState("English");
  const [timezone, setTimezone] = useState("Asia/Kolkata");

  // Link patient form state
  const [linkPublicId, setLinkPublicId] = useState("");

  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState("");

  useEffect(() => {
    if (searchParams.get("add") === "true") {
      setShowModal(true);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  function closeModal() {
    setShowModal(false);
    setFormError("");
    setDisplayName("");
    setDob("");
    setGender("");
    setLanguage("English");
    setTimezone("Asia/Kolkata");
    setLinkPublicId("");
  }

  async function handleCreate(event) {
    event.preventDefault();
    if (!displayName.trim()) return;
    setFormLoading(true);
    setFormError("");
    try {
      const payload = {
        display_name: displayName.trim(),
        date_of_birth: dob || null,
        gender: gender || null,
        preferred_language: language.trim() || "English",
        timezone: timezone.trim() || "Asia/Kolkata",
      };
      const created = await data.createPatient(payload);
      closeModal();
      navigate(`/app/patients/${created.id}`);
    } catch (err) {
      setFormError(err.message || "Failed to create patient");
    } finally {
      setFormLoading(false);
    }
  }

  async function handleLink(event) {
    event.preventDefault();
    if (!linkPublicId.trim()) return;
    setFormLoading(true);
    setFormError("");
    try {
      const linked = await data.linkPatient(linkPublicId.trim().toUpperCase());
      closeModal();
      navigate(`/app/patients/${linked.id}`);
    } catch (err) {
      setFormError(err.message || "Failed to link patient");
    } finally {
      setFormLoading(false);
    }
  }

  if (data.loading) return <LoadingState message="Loading assigned patients…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;

  return (
    <>
      <PageIntro
        eyebrow="Caregiver portal"
        title="Patients"
        action={
          <button
            type="button"
            className="button primary"
            onClick={() => setShowModal(true)}
            style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
          >
            + Add Patient
          </button>
        }
      />

      <div className="patient-list-grid">
        {data.emptyPatients ? (
          <EmptyPatientsState onAddPatient={() => setShowModal(true)} />
        ) : (
          <>
            {data.patients.map((patient) => (
              <Link className="patient-list-card" key={patient.id} to={`/app/patients/${patient.id}`}>
                <div className="avatar patient large">{initials(patient.display_name)}</div>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
                    <span className="eyebrow" style={{ margin: 0 }}>
                      Assigned patient
                    </span>
                    {patient.public_id && <span className="public-id-badge">{patient.public_id}</span>}
                  </div>
                  <h2>{patient.display_name}</h2>
                  <p>
                    {patient.preferred_language || "English"} · {patient.timezone || "Local time"}
                  </p>
                </div>
              </Link>
            ))}
            <button
              type="button"
              className="patient-list-card"
              onClick={() => setShowModal(true)}
              style={{
                borderStyle: "dashed",
                borderColor: "#b8d6c8",
                background: "#f9fcfb",
                cursor: "pointer",
                textAlign: "left",
              }}
            >
              <div
                className="avatar"
                style={{
                  width: "54px",
                  height: "54px",
                  fontSize: "20px",
                  background: "#eaf3ee",
                  color: "#185a4e",
                  border: "1px dashed #70aa94",
                }}
              >
                +
              </div>
              <div>
                <span className="eyebrow" style={{ margin: "0 0 4px", color: "#6a8880" }}>
                  Add Patient
                </span>
                <h2 style={{ fontSize: "16px", color: "#185a4e" }}>Connect new patient</h2>
                <p>Create a patient or link using Public ID</p>
              </div>
            </button>
          </>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={closeModal} role="dialog" aria-modal="true">
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">Patient Management</span>
                <h2>Add Patient</h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={closeModal}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <div className="tab-group">
              <button
                type="button"
                className={`tab-button ${tab === "create" ? "active" : ""}`}
                onClick={() => {
                  setTab("create");
                  setFormError("");
                }}
              >
                Create New Patient
              </button>
              <button
                type="button"
                className={`tab-button ${tab === "link" ? "active" : ""}`}
                onClick={() => {
                  setTab("link");
                  setFormError("");
                }}
              >
                Link Existing Patient
              </button>
            </div>

            {tab === "create" ? (
              <form onSubmit={handleCreate} className="auth-form">
                <div className="info-callout">
                  <p>
                    <strong>Primary Caregiver Assignment:</strong> You will become the primary caregiver for this patient and receive a public identifier (PT-XXXXXXXX).
                  </p>
                </div>

                <label>
                  Display Name *
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="e.g. Ramesh Sharma"
                    required
                  />
                </label>

                <label>
                  Date of Birth
                  <input
                    type="date"
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                  />
                </label>

                <label>
                  Gender
                  <select
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                    className="auth-select"
                  >
                    <option value="">Select gender (optional)</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                    <option value="Prefer not to say">Prefer not to say</option>
                  </select>
                </label>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  <label>
                    Preferred Language
                    <input
                      type="text"
                      value={language}
                      onChange={(e) => setLanguage(e.target.value)}
                      placeholder="English"
                    />
                  </label>
                  <label>
                    Timezone
                    <input
                      type="text"
                      value={timezone}
                      onChange={(e) => setTimezone(e.target.value)}
                      placeholder="Asia/Kolkata"
                    />
                  </label>
                </div>

                {formError && (
                  <p className="form-error" role="alert">
                    {formError}
                  </p>
                )}

                <div className="modal-footer">
                  <button type="button" className="button secondary" onClick={closeModal} disabled={formLoading}>
                    Cancel
                  </button>
                  <button type="submit" className="button primary" disabled={formLoading}>
                    {formLoading ? "Creating…" : "Create Patient"}
                  </button>
                </div>
              </form>
            ) : (
              <form onSubmit={handleLink} className="auth-form">
                <div className="info-callout">
                  <p>
                    <strong>Patient Linking:</strong> Enter the patient's public ID (PT-XXXXXXXX). If this patient does not yet have an assigned primary caregiver, you will become their Primary Caregiver. If a primary caregiver already exists, you will connect as a Secondary Caregiver.
                  </p>
                </div>

                <label>
                  Patient Public ID *
                  <input
                    type="text"
                    value={linkPublicId}
                    onChange={(e) => setLinkPublicId(e.target.value.toUpperCase())}
                    placeholder="PT-XXXXXXXX"
                    maxLength={30}
                    required
                  />
                </label>

                {formError && (
                  <p className="form-error" role="alert">
                    {formError}
                  </p>
                )}

                <div className="modal-footer">
                  <button type="button" className="button secondary" onClick={closeModal} disabled={formLoading}>
                    Cancel
                  </button>
                  <button type="submit" className="button primary" disabled={formLoading}>
                    {formLoading ? "Linking…" : "Link Patient"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
}

