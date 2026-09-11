import { useOutletContext } from "react-router-dom";
import { PageIntro } from "../../components/caregiver/CaregiverWidgets";

export default function ProfilePage({ caregiver, onSignOut }) {
  useOutletContext();
  return <><PageIntro eyebrow="Account" title="Caregiver profile" /><section className="profile-page-card"><div className="avatar doctor profile-avatar">{caregiver?.display_name?.split(/\s+/).map((part) => part[0]).slice(0, 2).join("") || "CC"}</div><div><span className="eyebrow">Authenticated caregiver</span><h2>{caregiver?.display_name || "Caregiver"}</h2><p>{caregiver?.role || "CAREGIVER"} · {caregiver?.email || "Demo account"}</p></div><button className="button secondary" type="button" onClick={onSignOut}>Sign out</button></section></>;
}
