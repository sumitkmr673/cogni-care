import { Icon } from "./Icons";

export default function StatCard({ icon, label, value, suffix, tone = "mint" }) {
  return (
    <article className={`stat-card ${tone}`}>
      <div className="stat-icon"><Icon name={icon} size={19} /></div>
      <div>
        <p>{label}</p>
        <strong>{value}{suffix && <small>{suffix}</small>}</strong>
      </div>
    </article>
  );
}
